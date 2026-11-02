// Los tests comprueban los textos en español: el navegador simulado (jsdom) dice ser en-US, así que
// se fija el idioma aquí. Los tests en inglés lo eligen proporcionando el token IDIOMA.
Object.defineProperty(navigator, 'languages', { value: ['es-ES'], configurable: true });
Object.defineProperty(navigator, 'language', { value: 'es-ES', configurable: true });
