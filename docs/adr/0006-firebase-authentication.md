# ADR-0006: Firebase Authentication en lugar de JWT propio

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

El monolito emitía sus propios JWT (firmados con un secreto en `application.properties`),
guardaba hashes BCrypt y exponía `/api/auth/login` y `/api/auth/registro`. Eso obligaba a mantener
recuperación de contraseña, verificación de email, rotación del secreto y login social por nuestra
cuenta. El secreto, además, estaba en el repositorio.

## Decisión

- La identidad la gestiona **Firebase Authentication** (email/contraseña y Google).
  El frontend obtiene el *ID token* con AngularFire y lo envía como `Authorization: Bearer`.
- El backend es un *resource server*: `FirebaseAuthenticationFilter` verifica el token con el
  Admin SDK (`verifyIdToken`) y crea una `FirebaseAuthentication` con el rol como authority.
  Sin token la petición es anónima; con token inválido, 401.
- Los roles (`USER`, `ADMIN`) son un **custom claim** `role`. Solo un ADMIN puede cambiarlo,
  vía `PUT /api/v1/admin/usuarios/{uid}/rol`, que llama a `setCustomUserClaims`.
- La aplicación no ve Spring Security: los controladores reciben un `Actor` (uid + rol) con
  `@CurrentActor`, y los casos de uso deciden permisos con él.
- El usuario local (`usuarios`) se identifica por el UID de Firebase y se crea en su primera
  petición autenticada.
- Credenciales: Application Default Credentials (cuenta de servicio de Cloud Run o
  `GOOGLE_APPLICATION_CREDENTIALS`). Nunca ficheros en el repositorio.
- Desarrollo: Firebase Emulator Suite con un proyecto `demo-benchify`. Tests: un
  `IdTokenVerifier` simulado que acepta tokens `test:<uid>[:ROL]`.

## Consecuencias

- Desaparecen el almacenamiento de contraseñas y el secreto JWT.
- El rol viaja en el token: tras cambiarlo, el usuario debe refrescar su token
  (`getIdToken(true)`) para que el backend vea el nuevo rol.
- No se comprueba la revocación del token en cada petición (`checkRevoked=false`) para evitar
  una llamada de red por request; la caducidad de una hora acota el riesgo.
- El Admin SDK solo detecta el emulador por variable de entorno; el perfil `dev` la fija desde
  `benchify.firebase.emulator.auth-host`.
