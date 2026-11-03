import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatChipListbox, MatChipOption } from '@angular/material/chips';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { toApiError } from '../../../core/api/api-error';
import { BancosApi } from '../../../core/api/bancos-api';
import { Banco, BancoRequest, EdicionPendiente, MaterialBanco } from '../../../core/api/models';
import { AuthService } from '../../../core/auth/auth.service';
import { GeolocationService } from '../../../core/geo/geolocation.service';
import { textos } from '../../../core/i18n/idioma';
import { Notifier } from '../../../core/ui/notifier';
import { Punto } from '../../../shared/map/bench-map';
import { LocationPicker, redondear } from '../../../shared/map/location-picker';
import { Confirmation } from '../../../shared/ui/confirm-dialog';
import { ErrorState } from '../../../shared/ui/error-state';
import { LoadingState } from '../../../shared/ui/loading-state';
import { GaleriaImagenes } from '../detalle/galeria-imagenes';
import { MATERIALES } from '../detalle/banco-detalle-page';
import { BANCO_FORM } from './banco-form-page.i18n';
import { ConCambiosSinGuardar } from './cambios-sin-guardar.guard';

export const MAX_ETIQUETAS = 5;

export const maxEtiquetasValidator: ValidatorFn = (control: AbstractControl<number[]>): ValidationErrors | null =>
  (control.value?.length ?? 0) > MAX_ETIQUETAS ? { demasiadas: true } : null;

@Component({
  selector: 'bf-banco-form-page',
  imports: [
    ReactiveFormsModule, RouterLink, LocationPicker, GaleriaImagenes, LoadingState, ErrorState,
    MatFormField, MatLabel, MatInput, MatError, MatHint, MatSelect, MatOption, MatCheckbox,
    MatChipListbox, MatChipOption, MatButton, MatIcon, DatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './banco-form-page.html',
  styleUrl: './banco-form-page.scss',
  host: { '(window:beforeunload)': 'avisarAlCerrar($event)' },
})
export class BancoFormPage implements ConCambiosSinGuardar {
  private readonly api = inject(BancosApi);
  private readonly geo = inject(GeolocationService);
  private readonly notifier = inject(Notifier);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly confirmation = inject(Confirmation);
  private readonly destroyRef = inject(DestroyRef);

  /** Presente al editar (/bancos/:id/editar). */
  readonly id = input<string>();

  protected readonly t = textos(BANCO_FORM);
  protected readonly materiales = Object.entries(MATERIALES) as [MaterialBanco, string][];
  protected readonly maxEtiquetas = MAX_ETIQUETAS;
  protected readonly etiquetas = toSignal(this.api.etiquetas(), { initialValue: [] });
  protected readonly centro: Punto = { lat: environment.map.center[0], lng: environment.map.center[1] };

  protected readonly ubicacion = signal<Punto | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly localizando = signal(false);
  /** Tras crear el banco se ofrece subir fotos antes de ir a la ficha. */
  protected readonly creado = signal<Banco | null>(null);
  protected readonly editando = computed(() => !!this.id());
  /** El autor edita un banco publicado: los cambios esperan moderación y el banco sigue igual mientras. */
  protected readonly conRevision = signal(false);
  /** Edición anterior aún sin revisar; la que se guarde ahora la sustituye. */
  protected readonly edicionPendiente = signal<EdicionPendiente | null>(null);
  /** Versión del banco al cargarlo (ETag): si otra persona lo cambia antes de guardar, el backend avisa. */
  private etag: string | null = null;

  protected readonly form = inject(NonNullableFormBuilder).group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.maxLength(500)]],
    lat: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    lng: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]],
    direccion: ['', [Validators.maxLength(255)]],
    ciudad: ['', [Validators.maxLength(100)]],
    provincia: ['', [Validators.maxLength(100)]],
    conSombra: [false],
    accesible: [false],
    conRespaldo: [false],
    material: [null as MaterialBanco | null],
    etiquetaIds: [[] as number[], [maxEtiquetasValidator]],
  });

  constructor() {
    // Mapa → formulario
    effect(() => {
      const punto = this.ubicacion();
      if (punto && (punto.lat !== this.form.controls.lat.value || punto.lng !== this.form.controls.lng.value)) {
        this.form.patchValue({ lat: punto.lat, lng: punto.lng });
        this.form.controls.lat.markAsDirty();
      }
    });
    // Formulario → mapa (coordenadas escritas a mano)
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(({ lat, lng }) => {
      if (lat != null && lng != null && this.form.controls.lat.valid && this.form.controls.lng.valid) {
        const actual = this.ubicacion();
        if (!actual || actual.lat !== lat || actual.lng !== lng) {
          this.ubicacion.set({ lat, lng });
        }
      }
    });
    effect(() => {
      const id = this.id();
      if (id) {
        void this.cargar(id);
      }
    });
  }

  protected async usarMiUbicacion(): Promise<void> {
    this.localizando.set(true);
    try {
      this.ubicacion.set(redondear(await this.geo.posicionActual()));
    } catch (error) {
      this.notifier.error(error instanceof Error ? error.message : this.t.sinUbicacion);
    } finally {
      this.localizando.set(false);
    }
  }

  protected async guardar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notifier.error(this.form.controls.lat.invalid
        ? this.t.marcaEnMapa
        : this.t.revisaCampos);
      return;
    }
    this.guardando.set(true);
    const request = this.toRequest();
    try {
      const id = this.id();
      if (id) {
        const { pendienteDeRevision } = await firstValueFrom(this.api.editar(id, request, this.etag));
        this.form.markAsPristine();
        this.notifier.success(pendienteDeRevision
          ? this.t.cambiosEnviados
          : this.t.cambiosGuardados);
        await this.router.navigate(['/bancos', id]);
      } else {
        const banco = await firstValueFrom(this.api.proponer(request));
        this.form.markAsPristine();
        this.creado.set(banco);
        this.notifier.success(banco.estado === 'PUBLICADO'
          ? this.t.bancoPublicado
          : this.t.bancoEnviado);
      }
    } catch (error) {
      this.aplicarErroresDeCampo(error);
      this.notifier.error(error);
    } finally {
      this.guardando.set(false);
    }
  }

  /** Pregunta antes de abandonar el formulario con cambios sin guardar (guard de la ruta). */
  puedeSalir(): boolean | Promise<boolean> {
    if (!this.form.dirty || this.guardando()) {
      return true;
    }
    return this.confirmation.ask({
      titulo: this.t.salirTitulo,
      mensaje: this.t.salirMensaje,
      confirmar: this.t.salir,
      peligroso: true,
    });
  }

  /** Al cerrar o recargar la pestaña el navegador pregunta si hay cambios sin guardar. */
  protected avisarAlCerrar(event: BeforeUnloadEvent): void {
    if (this.form.dirty) {
      event.preventDefault();
    }
  }

  private async cargar(id: string): Promise<void> {
    this.cargando.set(true);
    try {
      const { banco, etag } = await firstValueFrom(this.api.obtenerParaEditar(id));
      if (!this.auth.isAdmin() && banco.autor.id !== this.auth.session()?.uid) {
        this.errorCarga.set(this.t.sinPermiso);
        return;
      }
      this.etag = etag;
      if (!this.auth.isAdmin() && banco.estado === 'PUBLICADO') {
        this.conRevision.set(true);
        // Sin esperar: el aviso de una edición anterior no debe retrasar el formulario
        this.api.edicionPendiente(id).pipe(catchError(() => of(null)), takeUntilDestroyed(this.destroyRef))
          .subscribe((pendiente) => this.edicionPendiente.set(pendiente));
      }
      this.form.patchValue({
        titulo: banco.titulo,
        descripcion: banco.descripcion ?? '',
        lat: banco.lat,
        lng: banco.lng,
        direccion: banco.direccion ?? '',
        ciudad: banco.ciudad ?? '',
        provincia: banco.provincia ?? '',
        conSombra: banco.conSombra,
        accesible: banco.accesible,
        conRespaldo: banco.conRespaldo,
        material: banco.material,
        etiquetaIds: banco.etiquetas.map((e) => e.id),
      });
      this.ubicacion.set({ lat: banco.lat, lng: banco.lng });
      this.form.markAsPristine();
    } catch (error) {
      this.errorCarga.set(toApiError(error).message);
    } finally {
      this.cargando.set(false);
    }
  }

  private toRequest(): BancoRequest {
    const v = this.form.getRawValue();
    const texto = (s: string) => s.trim() || null;
    return {
      titulo: v.titulo.trim(),
      descripcion: texto(v.descripcion),
      lat: v.lat!,
      lng: v.lng!,
      direccion: texto(v.direccion),
      ciudad: texto(v.ciudad),
      provincia: texto(v.provincia),
      conSombra: v.conSombra,
      accesible: v.accesible,
      conRespaldo: v.conRespaldo,
      material: v.material,
      etiquetaIds: v.etiquetaIds,
    };
  }

  /** Marca en el formulario los campos que el backend ha rechazado. */
  private aplicarErroresDeCampo(error: unknown): void {
    for (const { field, message } of toApiError(error).fieldErrors) {
      const control = this.form.get(field);
      control?.setErrors({ servidor: message });
      control?.markAsTouched();
    }
  }
}
