CREATE TABLE pacientes (
    id                          UUID PRIMARY KEY,
    nombre_completo             VARCHAR(100)  NOT NULL,
    edad                        SMALLINT      NOT NULL CHECK (edad BETWEEN 0 AND 120),
    frecuencia_cardiaca         SMALLINT      NOT NULL CHECK (frecuencia_cardiaca BETWEEN 20 AND 250),
    presion_sistolica           SMALLINT      NOT NULL CHECK (presion_sistolica BETWEEN 60 AND 260),
    presion_diastolica          SMALLINT      NOT NULL CHECK (presion_diastolica BETWEEN 30 AND 160),
    spo2                        SMALLINT      NOT NULL CHECK (spo2 BETWEEN 70 AND 100),
    temperatura                 NUMERIC(4, 1) NOT NULL CHECK (temperatura BETWEEN 34.0 AND 42.0),
    dolor                       SMALLINT      NOT NULL CHECK (dolor BETWEEN 0 AND 10),
    gcs                         SMALLINT      NOT NULL CHECK (gcs BETWEEN 3 AND 15),
    queja_principal             TEXT          NOT NULL,
    nivel                       VARCHAR(20)   NOT NULL,
    estado                      VARCHAR(20)   NOT NULL,
    escalado                    BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_registro              TIMESTAMPTZ   NOT NULL,
    fecha_ultimo_cambio_estado  TIMESTAMPTZ   NOT NULL,
    fecha_entrada_consulta      TIMESTAMPTZ,
    fecha_alta                  TIMESTAMPTZ,
    fecha_escalamiento          TIMESTAMPTZ,
    version                     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_presion CHECK (presion_diastolica < presion_sistolica)
);

CREATE INDEX idx_pacientes_en_espera ON pacientes (fecha_registro)
    WHERE estado = 'REGISTRADO' AND escalado = FALSE;

CREATE INDEX idx_pacientes_fecha_registro ON pacientes (fecha_registro);

CREATE TABLE auditoria_clinica (
    id              UUID PRIMARY KEY,
    paciente_id     UUID        NOT NULL REFERENCES pacientes (id),
    "timestamp"     TIMESTAMPTZ NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20) NOT NULL,
    tipo_evento     VARCHAR(30) NOT NULL
);

CREATE INDEX idx_auditoria_paciente ON auditoria_clinica (paciente_id, "timestamp");
CREATE INDEX idx_auditoria_tipo_evento ON auditoria_clinica (tipo_evento, paciente_id, "timestamp");

CREATE OR REPLACE FUNCTION impedir_modificacion_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría clínica es inmutable: no se permite % sobre auditoria_clinica', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_clinica
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_auditoria();
