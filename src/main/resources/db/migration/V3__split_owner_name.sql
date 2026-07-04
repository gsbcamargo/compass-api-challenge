ALTER TABLE accounts ADD COLUMN first_name VARCHAR(255);
ALTER TABLE accounts ADD COLUMN last_name VARCHAR(255);

UPDATE accounts SET
    first_name = split_part(owner_name, ' ', 1),
    last_name = CASE
                    WHEN position(' ' IN owner_name) > 0
                        THEN substring(owner_name FROM position(' ' IN owner_name) + 1)
                    ELSE ''
        END;

ALTER TABLE accounts ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE accounts ALTER COLUMN last_name SET NOT NULL;

ALTER TABLE accounts DROP COLUMN owner_name;
