/*
 * Carrito del sitio público. Vive en el navegador (localStorage, una clave por comercio) y al
 * confirmar se envía un formulario común a /pedido con ids y cantidades. Los precios que se ven
 * acá son solo informativos: el servidor vuelve a calcular todo con los precios de la base.
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
        });
    });

    const formulario = dialogo.querySelector('[data-form-pedido]');
    const barra = document.querySelector('[data-barra-pedido]');
    let carrito = leer();

    function leer() {
        try {
            const guardado = JSON.parse(localStorage.getItem(CLAVE) || '{}');
            // Se descartan productos que ya no existen o quedaron sin stock.
            return Object.fromEntries(Object.entries(guardado)
                .filter(([id, cantidad]) => productos.has(id) && Number.isInteger(cantidad) && cantidad > 0));
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

    function cambiar(id, delta) {
        const cantidad = Math.min(MAXIMO, Math.max(0, (carrito[id] || 0) + delta));
        if (cantidad === 0) {
            delete carrito[id];
        } else {
            carrito[id] = cantidad;
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

    function botonAgregar(nombre) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'boton boton--agregar';
        boton.dataset.accion = 'sumar';
        boton.setAttribute('aria-label', 'Agregar ' + nombre);
        boton.textContent = 'Agregar';
        return boton;
    }

    function dibujar() {
        let unidades = 0;
        let total = 0;
        const lineas = dialogo.querySelector('[data-lineas]');
        lineas.replaceChildren();

        productos.forEach((producto, id) => {
            const cantidad = carrito[id] || 0;
            producto.accion.replaceChildren(cantidad > 0
                ? controlDeCantidad(producto.nombre, cantidad)
                : botonAgregar(producto.nombre));
            if (cantidad === 0) return;

            unidades += cantidad;
            total += cantidad * producto.precio;

            const linea = document.createElement('li');
            linea.className = 'linea';
            linea.dataset.id = id;
            const nombre = document.createElement('span');
            nombre.className = 'linea__nombre';
            nombre.textContent = producto.nombre;
            const subtotal = document.createElement('span');
            subtotal.className = 'linea__subtotal';
            subtotal.textContent = pesos(cantidad * producto.precio);
            linea.append(nombre, controlDeCantidad(producto.nombre, cantidad), subtotal);
            lineas.append(linea);
        });

        document.querySelectorAll('[data-total]').forEach((el) => { el.textContent = pesos(total); });
        barra.querySelector('[data-resumen]').textContent =
            '(' + unidades + (unidades === 1 ? ' producto)' : ' productos)');
        barra.hidden = unidades === 0;
        if (unidades === 0 && dialogo.open) dialogo.close();
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
        } else if (accion === 'cerrar-pedido') {
            dialogo.close();
        } else if (accion === 'sumar' || accion === 'restar') {
            const id = boton.closest('[data-id]')?.dataset.id;
            if (id && productos.has(id)) cambiar(id, accion === 'sumar' ? 1 : -1);
        }
    });

    // Tocar fuera del diálogo (en el fondo oscuro) lo cierra.
    dialogo.addEventListener('click', (evento) => {
        if (evento.target === dialogo) dialogo.close();
    });

    formulario.addEventListener('change', (evento) => {
        if (evento.target.name === 'modalidad') actualizarModalidad();
    });

    formulario.addEventListener('submit', (evento) => {
        const ids = Object.keys(carrito);
        if (ids.length === 0) {
            evento.preventDefault();
            return;
        }
        // Spring arma la lista "lineas" a partir de nombres como lineas[0].productoId.
        const campos = formulario.querySelector('[data-campos-lineas]');
        campos.replaceChildren();
        ids.forEach((id, i) => {
            campos.append(oculto('lineas[' + i + '].productoId', id), oculto('lineas[' + i + '].cantidad', carrito[id]));
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
