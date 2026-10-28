/*
 * Carrito del sitio público. Vive en el navegador (localStorage, una clave por comercio) y al
 * confirmar se envía un formulario común a /pedido con ids y cantidades. Los precios que se ven
 * acá son solo informativos: el servidor vuelve a calcular todo con los precios de la base.
 *
 * Cada renglón del carrito se identifica con una "clave": el id del producto ("12") o, si el
 * producto tiene talles/colores, el id del producto y el de la variante ("12:340").
 */
(() => {
    'use strict';

    const slug = document.body.dataset.carrito;
    const dialogo = document.querySelector('[data-dialogo-pedido]');
    if (!slug || !dialogo) return;

    const CLAVE = 'vidriera:carrito:' + slug;
    const MAXIMO = 99;
    const numero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });
    const pesos = (importe) => '$ ' + numero.format(importe);

    // Productos que se pueden pedir, leídos de los atributos data-* que pone el servidor.
    const productos = new Map();
    document.querySelectorAll('.producto[data-id]').forEach((elemento) => {
        if (elemento.dataset.pedible !== 'true') return;
        productos.set(elemento.dataset.id, {
            nombre: elemento.dataset.nombre,
            precio: Number(elemento.dataset.precio),
            accion: elemento.querySelector('[data-accion-producto]'),
            selector: elemento.querySelector('[data-variante]'),
        });
    });

    const formulario = dialogo.querySelector('[data-form-pedido]');
    const barra = document.querySelector('[data-barra-pedido]');
    let carrito = leer();

    /** "12:340" → { id: "12", variante: "340" }. */
    function partes(clave) {
        const [id, variante] = clave.split(':');
        return { id, variante: variante || null };
    }

    /** Texto de la variante elegida, sin el aviso "(sin stock)". */
    function etiquetaVariante(producto, variante) {
        const opcion = producto.selector && [...producto.selector.options].find((o) => o.value === variante);
        return opcion ? opcion.textContent.trim() : null;
    }

    function claveValida(clave, cantidad) {
        const { id, variante } = partes(clave);
        const producto = productos.get(id);
        if (!producto || !Number.isInteger(cantidad) || cantidad <= 0) return false;
        if (!producto.selector) return variante === null;
        const opcion = [...producto.selector.options].find((o) => o.value === variante);
        return Boolean(opcion && !opcion.disabled);
    }

    function leer() {
        try {
            const guardado = JSON.parse(localStorage.getItem(CLAVE) || '{}');
            // Se descartan productos o talles que ya no existen o quedaron sin stock.
            return Object.fromEntries(Object.entries(guardado).filter(([clave, cantidad]) => claveValida(clave, cantidad)));
        } catch {
            return {};
        }
    }

    function guardar() {
        try {
            localStorage.setItem(CLAVE, JSON.stringify(carrito));
        } catch {
            // Modo privado o almacenamiento lleno: el carrito funciona igual hasta recargar la página.
        }
    }

    function cambiar(clave, delta) {
        const cantidad = Math.min(MAXIMO, Math.max(0, (carrito[clave] || 0) + delta));
        if (cantidad === 0) {
            delete carrito[clave];
        } else {
            carrito[clave] = cantidad;
        }
        guardar();
        dibujar();
    }

    function controlDeCantidad(nombre, cantidad) {
        const control = document.createElement('div');
        control.className = 'cantidad';
        control.innerHTML =
            '<button type="button" class="cantidad__boton" data-accion="restar">−</button>' +
            '<span class="cantidad__valor"></span>' +
            '<button type="button" class="cantidad__boton" data-accion="sumar">+</button>';
        control.querySelector('.cantidad__valor').textContent = cantidad;
        control.querySelector('[data-accion="restar"]').setAttribute('aria-label', 'Quitar uno de ' + nombre);
        control.querySelector('[data-accion="sumar"]').setAttribute('aria-label', 'Agregar uno de ' + nombre);
        return control;
    }

    function botonAgregar(nombre, enPedido) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'boton boton--agregar';
        boton.dataset.accion = 'sumar';
        boton.setAttribute('aria-label', 'Agregar ' + nombre);
        boton.textContent = enPedido > 0 ? 'Agregar (' + enPedido + ' en el pedido)' : 'Agregar';
        return boton;
    }

    function dibujar() {
        let unidades = 0;
        let total = 0;
        const enPedido = new Map(); // id de producto → unidades (sumando todos sus talles)

        const lineas = dialogo.querySelector('[data-lineas]');
        lineas.replaceChildren();
        Object.entries(carrito).forEach(([clave, cantidad]) => {
            const { id, variante } = partes(clave);
            const producto = productos.get(id);
            unidades += cantidad;
            total += cantidad * producto.precio;
            enPedido.set(id, (enPedido.get(id) || 0) + cantidad);

            const nombreCompleto = variante
                ? producto.nombre + ' (' + etiquetaVariante(producto, variante) + ')'
                : producto.nombre;
            const linea = document.createElement('li');
            linea.className = 'linea';
            linea.dataset.clave = clave;
            const nombre = document.createElement('span');
            nombre.className = 'linea__nombre';
            nombre.textContent = nombreCompleto;
            const subtotal = document.createElement('span');
            subtotal.className = 'linea__subtotal';
            subtotal.textContent = pesos(cantidad * producto.precio);
            linea.append(nombre, controlDeCantidad(nombreCompleto, cantidad), subtotal);
            lineas.append(linea);
        });

        // En la tarjeta: sin talles, el control de cantidad; con talles, "Agregar" (cada talle es otra línea).
        productos.forEach((producto, id) => {
            const cantidad = enPedido.get(id) || 0;
            producto.accion.replaceChildren(!producto.selector && cantidad > 0
                ? controlDeCantidad(producto.nombre, cantidad)
                : botonAgregar(producto.nombre, producto.selector ? cantidad : 0));
        });

        document.querySelectorAll('[data-total]').forEach((el) => { el.textContent = pesos(total); });
        barra.querySelector('[data-resumen]').textContent =
            '(' + unidades + (unidades === 1 ? ' producto)' : ' productos)');
        barra.hidden = unidades === 0;
        if (unidades === 0 && dialogo.open) dialogo.close();
    }

    /** Clave del producto de una tarjeta; null si tiene talles y todavía no eligió uno. */
    function claveDeTarjeta(id) {
        const producto = productos.get(id);
        if (!producto.selector) return id;
        if (!producto.selector.value) {
            producto.selector.setCustomValidity('Elegí un talle o color');
            producto.selector.reportValidity();
            return null;
        }
        return id + ':' + producto.selector.value;
    }

    function actualizarModalidad() {
        const modalidad = formulario.querySelector('input[name="modalidad"]:checked')?.value;
        const envio = modalidad === 'ENVIO';
        formulario.querySelector('[data-solo-envio]').hidden = !envio;
        formulario.querySelector('[data-direccion]').required = envio;
        // En la mesa el nombre es opcional; para retiro y envío, obligatorio.
        formulario.querySelector('[data-nombre]').required = modalidad !== 'MESA';
    }

    document.addEventListener('click', (evento) => {
        const boton = evento.target.closest('[data-accion]');
        if (!boton) return;
        const accion = boton.dataset.accion;
        if (accion === 'abrir-pedido') {
            dialogo.showModal();
            return;
        }
        if (accion === 'cerrar-pedido') {
            dialogo.close();
            return;
        }
        if (accion !== 'sumar' && accion !== 'restar') return;
        const delta = accion === 'sumar' ? 1 : -1;

        const linea = boton.closest('[data-clave]');
        if (linea) {
            cambiar(linea.dataset.clave, delta);
            return;
        }
        const id = boton.closest('.producto[data-id]')?.dataset.id;
        if (!id || !productos.has(id)) return;
        const clave = claveDeTarjeta(id);
        if (clave) cambiar(clave, delta);
    });

    document.addEventListener('change', (evento) => {
        if (evento.target.matches('[data-variante]')) evento.target.setCustomValidity('');
    });

    // Tocar fuera del diálogo (en el fondo oscuro) lo cierra.
    dialogo.addEventListener('click', (evento) => {
        if (evento.target === dialogo) dialogo.close();
    });

    formulario.addEventListener('change', (evento) => {
        if (evento.target.name === 'modalidad') actualizarModalidad();
    });

    formulario.addEventListener('submit', (evento) => {
        const claves = Object.keys(carrito);
        if (claves.length === 0) {
            evento.preventDefault();
            return;
        }
        // Spring arma la lista "lineas" a partir de nombres como lineas[0].productoId.
        const campos = formulario.querySelector('[data-campos-lineas]');
        campos.replaceChildren();
        claves.forEach((clave, i) => {
            const { id, variante } = partes(clave);
            campos.append(oculto('lineas[' + i + '].productoId', id), oculto('lineas[' + i + '].cantidad', carrito[clave]));
            if (variante) campos.append(oculto('lineas[' + i + '].varianteId', variante));
        });
        carrito = {};
        guardar();
    });

    function oculto(nombre, valor) {
        const campo = document.createElement('input');
        campo.type = 'hidden';
        campo.name = nombre;
        campo.value = valor;
        return campo;
    }

    // Si el cliente vuelve con el botón "atrás", el navegador puede mostrar la página guardada en
    // memoria: se vuelve a leer el carrito (que se vació al enviar).
    window.addEventListener('pageshow', (evento) => {
        if (evento.persisted) {
            carrito = leer();
            dibujar();
        }
    });

    actualizarModalidad();
    dibujar();
})();
