package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.Autor;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.banco.domain.Valoracion;
import org.mapstruct.Mapper;

import java.util.Set;

/**
 * MapStruct genera la conversión de etiquetas. El agregado {@link Banco} expone su estado con
 * accesores de estilo record (no JavaBean) y se compone de varios value objects, así que su
 * traducción se escribe aquí de forma explícita.
 */
@Mapper
interface BancoEntityMapper {

    Etiqueta toDomain(EtiquetaEntity entity);

    Set<Etiqueta> toDomain(Set<EtiquetaEntity> entities);

    default Banco toDomain(BancoEntity entity) {
        DatosBanco datos = new DatosBanco(
                entity.getTitulo(),
                entity.getDescripcion(),
                new Ubicacion(entity.getLat(), entity.getLng()),
                new DireccionPostal(entity.getDireccion(), entity.getCiudad(), entity.getProvincia(), entity.getPais()),
                new Caracteristicas(entity.isConSombra(), entity.isAccesible(), entity.isConRespaldo(),
                        entity.getMaterial()),
                toDomain(entity.getEtiquetas()));
        return new Banco(entity.getId(), datos, entity.getEstado(),
                new Autor(entity.getCreadoPor(), entity.getAutorNombre()),
                new Valoracion(entity.getPuntuacionMedia(), entity.getNumResenas()),
                entity.getFechaSubida(), entity.getFechaVerificacion(), entity.getVersion());
    }

    /** Copia el estado del agregado en una entidad nueva o gestionada; las etiquetas las fija el adaptador. */
    default void copyToEntity(Banco banco, BancoEntity entity) {
        DatosBanco datos = banco.datos();
        entity.setId(banco.id());
        entity.setTitulo(datos.titulo());
        entity.setDescripcion(datos.descripcion());
        entity.setLat(datos.ubicacion().lat());
        entity.setLng(datos.ubicacion().lng());
        entity.setDireccion(datos.direccion().direccion());
        entity.setCiudad(datos.direccion().ciudad());
        entity.setProvincia(datos.direccion().provincia());
        entity.setPais(datos.direccion().pais());
        entity.setConSombra(datos.caracteristicas().conSombra());
        entity.setAccesible(datos.caracteristicas().accesible());
        entity.setConRespaldo(datos.caracteristicas().conRespaldo());
        entity.setMaterial(datos.caracteristicas().material());
        entity.setEstado(banco.estado());
        entity.setPuntuacionMedia(banco.valoracion().media());
        entity.setNumResenas(banco.valoracion().numResenas());
        entity.setCreadoPor(banco.autor().id());
        entity.setFechaSubida(banco.fechaSubida());
        entity.setFechaVerificacion(banco.fechaVerificacion());
    }
}
