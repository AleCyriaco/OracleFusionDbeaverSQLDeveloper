SELECT
    usr.USERNAME                          AS USUARIO,
    usr.DISPLAY_NAME                      AS NOME_COMPLETO,
    usr.EMAIL                             AS EMAIL,
    role_mem.ROLE_NAME                    AS ROLE_CODE,
    role_mem.ROLE_DISPLAY_NAME            AS ROLE_NOME,
    role_mem.ROLE_CATEGORY                AS CATEGORIA,
    role_mem.ASSIGNMENT_TYPE              AS TIPO_ATRIBUICAO,
    role_mem.START_DATE                   AS DATA_INICIO,
    role_mem.END_DATE                     AS DATA_FIM,
    CASE
        WHEN role_mem.END_DATE IS NULL THEN 'Ativa'
        WHEN role_mem.END_DATE > SYSDATE THEN 'Ativa'
        ELSE 'Expirada'
    END                                   AS STATUS_ROLE
FROM
    PER_USERS usr
    INNER JOIN PER_USER_ROLES role_mem
        ON usr.USER_ID = role_mem.USER_ID
WHERE
    usr.USERNAME IN (
        'alensantos', 'alferrreira', 'aperes', 'aramonim', 'arrissilva',
        'bissilva', 'bmelo', 'cmsouza', 'dlima', 'dvieira',
        'fgomes', 'fpalacio', 'fribeiro', 'ggdias', 'gissantos',
        'gpmacedo', 'hcsoares', 'jorpsantos', 'jsialves', 'letdsilva',
        'machaves', 'mcecao', 'mdomenciano', 'mldalmeida', 'mpalma',
        'ndcmoreira', 'ratagiba', 'roscardoso', 'trana', 'visgomes',
        'vpita', 'ycarvalho'
    )
    AND (role_mem.END_DATE IS NULL OR role_mem.END_DATE > SYSDATE)
ORDER BY
    usr.USERNAME,
    role_mem.ROLE_DISPLAY_NAME