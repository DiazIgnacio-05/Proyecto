-- Si el comercio hace envíos a domicilio, el pedido por WhatsApp ofrece esa opción.
ALTER TABLE comercio ADD COLUMN ofrece_envio BOOLEAN NOT NULL DEFAULT FALSE;
