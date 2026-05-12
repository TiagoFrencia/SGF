-- Stable operational branches used by POS, transfers and frontend defaults

INSERT INTO branches (id, code, name, active)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'CENTRAL-OPS', 'Sucursal Central Operativa', true),
    ('00000000-0000-0000-0000-000000000202', 'NORTE-OPS', 'Sucursal Norte Operativa', true)
ON CONFLICT (code) DO NOTHING;
