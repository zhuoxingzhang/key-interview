from Key import Key


def schema_info_db(schema_name: str) -> tuple:
    schema = schema_name.lower()
    if schema == 'goalies_shootout':
        R = ["playerID", "year", "stint", "tmID", "W", "L", "SA", "GA"]
        ground_truth_minimal_keys = [
            Key(["playerID", "year", "stint"])
        ]

    elif schema == 'team_vs_team':
        R = ["year", "tmID", "oppID", "lgID", "W", "L", "T", "OTL"]
        ground_truth_minimal_keys = [
            Key(["year", "tmID", "oppID"])
        ]

    elif schema == 'teams_half':
        R = ["year", "tmID", "half", "lgID", "rank", "G", "W", "L", "T", "GF", "GA"]
        ground_truth_minimal_keys = [
            Key(["year", "tmID", "half"])
        ]

    elif schema == 'scoring_shootout':
        R = ["playerID", "year", "stint", "tmID", "S", "G", "GDG"]
        ground_truth_minimal_keys = [
            Key(["playerID", "year", "stint"])
        ]

    elif schema == 'awards_coaches':
        R = ["coachID", "award", "year", "lgID", "note"]
        ground_truth_minimal_keys = [
            Key(["coachID", "award", "year", "lgID"])
        ]

    elif schema == 'combined_shutouts':
        R = ["year", "month", "date", "tmID", "oppID", "R/P", "IDgoalie1", "IDgoalie2"]
        ground_truth_minimal_keys = [
            Key(["year", "month", "date", "tmID"]),
            Key(["year", "month", "date", "oppID"])
        ]

    elif schema == 'teams_post':
        R = [
            "year", "tmID", "lgID",
            "G", "W", "L", "T",
            "GF", "GA", "PIM", "BenchMinor",
            "PPG", "PPC", "SHA", "PKG", "PKC", "SHF"
        ]
        ground_truth_minimal_keys = [
            Key(["year", "tmID"])
        ]

    elif schema == 'goalies':
        R = [
            "playerID", "year", "stint", "tmID", "lgID",
            "GP", "Min", "W", "L", "T/OL",
            "ENG", "SHO", "GA", "SA",
            "PostGP", "PostMin", "PostW", "PostL", "PostT",
            "PostENG", "PostSHO", "PostGA", "PostSA"
        ]
        ground_truth_minimal_keys = [
            Key(["playerID", "year", "stint"])
        ]

    elif schema == 'team_splits':
        R = [
            "year", "tmID", "lgID",
            "hW", "hL", "hT", "hOTL",
            "rW", "rL", "rT", "rOTL",
            "SepW", "SepL", "SepT", "SepOL",
            "OctW", "OctL", "OctT", "OctOL",
            "NovW", "NovL", "NovT", "NovOL",
            "DecW", "DecL", "DecT", "DecOL",
            "JanW", "JanL", "JanT", "JanOL",
            "FebW", "FebL", "FebT", "FebOL",
            "MarW", "MarL", "MarT", "MarOL",
            "AprW", "AprL", "AprT", "AprOL"
        ]
        ground_truth_minimal_keys = [
            Key(["year", "tmID"])
        ]

    elif schema == 'abalone':
        R = [
            "sex", "length", "diameter", "height",
            "whole_weight", "shucked_weight", "viscera_weight",
            "shell_weight", "rings"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'breast':
        R = [
            "sample_code_number",
            "clump_thickness",
            "uniformity_of_cell_size",
            "uniformity_of_cell_shape",
            "marginal_adhesion",
            "single_epithelial_cell_size",
            "bare_nuclei",
            "bland_chromatin",
            "normal_nucleoli",
            "mitoses",
            "class"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'echo':
        R = [
            "survival",
            "still_alive",
            "age_at_heart_attack",
            "pericardial_effusion",
            "fractional_shortening",
            "epss",
            "lvdd",
            "wall_motion_score",
            "wall_motion_index",
            "mult",
            "name",
            "group",
            "alive_at_1"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'claims':
        R = [
            "claim_id",
            "member_id",
            "provider_id",
            "diagnosis_code",
            "procedure_code",
            "service_date",
            "received_date",
            "paid_date",
            "claim_type",
            "claim_status",
            "billed_amount",
            "allowed_amount",
            "paid_amount"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'hospital':
        R = [
            "provider_number",
            "hospital_name",
            "address",
            "city",
            "state",
            "zip_code",
            "county_name",
            "phone_number",
            "hospital_type",
            "hospital_ownership",
            "emergency_service"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'weather':
        R = [
            "station_id",
            "date",
            "time",
            "latitude",
            "longitude",
            "elevation",
            "temperature",
            "dew_point",
            "humidity",
            "pressure",
            "wind_direction",
            "wind_speed",
            "visibility",
            "precipitation",
            "snow_depth",
            "cloud_cover",
            "weather_condition",
            "quality_flag"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'routes':
        R = [
            "airline",
            "airline_id",
            "source_airport",
            "source_airport_id",
            "destination_airport",
            "destination_airport_id",
            "codeshare",
            "stops",
            "equipment"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'bridges':
        R = [
            "identif",
            "river",
            "location",
            "erected",
            "purpose",
            "length",
            "lanes",
            "clear_g",
            "t_or_d",
            "material",
            "span",
            "rel_l",
            "type"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'pdbx':
        R = [
            "pdb_id",
            "chain_id",
            "molecule_name",
            "organism",
            "taxonomy_id",
            "experimental_method",
            "resolution",
            "deposition_date",
            "release_date",
            "sequence_length",
            "structure_title",
            "classification",
            "keywords"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'adult':
        R = [
            "age",
            "workclass",
            "fnlwgt",
            "education",
            "education_num",
            "marital_status",
            "occupation",
            "relationship",
            "race",
            "sex",
            "capital_gain",
            "capital_loss",
            "hours_per_week",
            "native_country",
            "income"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'lineitem':
        R = [
            "l_orderkey",
            "l_partkey",
            "l_suppkey",
            "l_linenumber",
            "l_quantity",
            "l_extendedprice",
            "l_discount",
            "l_tax",
            "l_returnflag",
            "l_linestatus",
            "l_shipdate",
            "l_commitdate",
            "l_receiptdate",
            "l_shipinstruct",
            "l_shipmode",
            "l_comment"
        ]
        ground_truth_minimal_keys = []

    elif schema == 'ncvoter':
        R = [
            "voter_id",
            "county_id",
            "county_desc",
            "voter_reg_num",
            "status_cd",
            "voter_status_desc",
            "reason_cd",
            "voter_status_reason_desc",
            "res_street_address",
            "res_city",
            "res_state",
            "res_zip_code",
            "mail_addr1",
            "mail_city",
            "mail_state",
            "mail_zip_code",
            "sex_code",
            "race_code",
            "ethnic_code"
        ]
        ground_truth_minimal_keys = []

    else:
        raise ValueError(f"Unknown schema name: {schema_name}")

    return R, ground_truth_minimal_keys
