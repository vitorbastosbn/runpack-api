-- Seed conquistas iniciais do MVP
INSERT INTO achievement (id, slug, name, description) VALUES
  (gen_random_uuid(), 'first_run',        'Primeira corrida',       'Complete sua primeira corrida'),
  (gen_random_uuid(), 'first_group_run',  'Corrida em grupo',       'Complete uma corrida com pelo menos 2 participantes'),
  (gen_random_uuid(), 'five_runs',        'Veterano',               'Complete 5 corridas'),
  (gen_random_uuid(), 'ten_km_total',     '10 km acumulados',       'Acumule 10 km em corridas'),
  (gen_random_uuid(), 'fifty_km_total',   '50 km acumulados',       'Acumule 50 km em corridas'),
  (gen_random_uuid(), 'three_weeks_streak','3 semanas consecutivas', 'Corra pelo menos uma vez por semana por 3 semanas seguidas'),
  (gen_random_uuid(), 'podium',           'Pódio',                  'Termine em 1º lugar em uma corrida com 3 ou mais participantes'),
  (gen_random_uuid(), 'fast_five',        'PR de 5 km',             'Complete 5 km em menos de 30 minutos');
