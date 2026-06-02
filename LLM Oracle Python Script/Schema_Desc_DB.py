def schema_desc_db(schema: str) -> str:
    """
    It is better to mention each attribute when introducing what is each tuple of the relation in the schema.
    Following this, introducing the meaning of each attribute of the schema.
    """
    if schema.lower() == 'goalies_shootout':
        schema_desc = (
            "- Each tuple represents a goalie’s shootout performance record for a specific team assignment within a given season year.\n"
            "- playerID: identifier of a goalie (player).\n"
            "- year: the season year in which the record is recorded.\n"
            "- stint: an integer indicating the sequence of the player's team assignments within a season year (e.g., 1 for first team, 2 for second team if traded).\n"
            "- tmID: identifier of the team the player was playing for.\n"
            "- W: number of shootout wins.\n"
            "- L: number of shootout losses.\n"
            "- SA: number of shootout shots against the goalie.\n"
            "- GA: number of shootout goals allowed.\n"
        )
    elif schema.lower() == 'awards_misc':  # table is weird, do NOT use
        schema_desc = (
            "- Each tuple represents an award record in a historical hockey statistics database. "
            "The table stores miscellaneous award-related information for players.\n"
            "- name: the name of the player or award recipient.\n"
            "- ID: a unique identifier assigned internally to each record.\n"
            "- award: the name of the award (e.g., MVP, Best Defenseman, Rookie of the Year).\n"
            "- year: the year in which the award was given.\n"
            "- lgID: league identifier (e.g., NHL or other leagues), indicating the league context of the award.\n"
            "- note: additional textual notes or remarks about the award.\n"
        )
    elif schema.lower() == 'student_course_enrollment':
        schema_desc = (
            "- Each tuple represents a student's enrollment in a course.\n"
            "- StudentID: identifier of a student.\n"
            "- StudentName: name of a student.\n"
            "- Course: course taken by a student.\n"
            "- StudentGrade: grade received in that course.\n"
            "- Semester: the semester in which the course was taken.\n"
        )
    elif schema.lower() == 'team_vs_team':
        schema_desc = (
            "- Each tuple represents the aggregated game results between one team and a specific opponent "
            "within a given league and season year.\n"
            "- year: the season year in which the games were played.\n"
            "- tmID: identifier of the team.\n"
            "- oppID: identifier of the opposing team.\n"
            "- lgID: identifier of the league in which the games took place (e.g., NHL).\n"
            "- W: number of games won by tmID against oppID.\n"
            "- L: number of games lost by tmID against oppID.\n"
            "- T: number of games that ended in a tie.\n"
            "- OTL: number of overtime losses (games lost in overtime).\n"
        )
    elif schema.lower() == 'teams_half':
        schema_desc = (
            "- Each tuple represents a team's performance statistics for a specific half of a season within a given league and year.\n"
            "- year: the season year in which the records are recorded.\n"
            "- tmID: identifier of the team.\n"
            "- half: indicates which half of the season the record refers to (e.g., first half or second half).\n"
            "- lgID: identifier of the league in which the games took place (e.g., NHL).\n"
            "- rank: the team's ranking within the league for that half-season period.\n"
            "- G: number of games played by the team in that half-season.\n"
            "- W: number of games won by the team.\n"
            "- L: number of games lost by the team.\n"
            "- T: number of games that ended in a tie.\n"
            "- GF: number of goals scored by the team (goals for).\n"
            "- GA: number of goals conceded by the team (goals against).\n"
        )
    elif schema.lower() == 'scoring_shootout':
        schema_desc = (
            "- Each tuple represents a player's performance record for a specific team assignment within a given season year.\n"
            "- playerID: identifier of a player.\n"
            "- year: the season year in which the record is recorded.\n"
            "- stint: an integer indicating the sequence of the player's team assignments within a season year "
            "(e.g., 1 for first team, 2 for second team if traded).\n"
            "- tmID: identifier of the team the player was playing for.\n"
            "- S: total shots taken by the player.\n"
            "- G: total goals scored by the player.\n"
            "- GDG: number of game-deciding goals scored by the player.\n"
        )
    elif schema.lower() == 'awards_coaches':
        schema_desc = (
            "- Each tuple represents an award received by a coach in a specific league and year.\n"
            "- coachID: identifier of a coach.\n"
            "- award: name of the award received (e.g., Coach of the Year).\n"
            "- year: the year in which the award was given.\n"
            "- lgID: identifier of the league in which the award was awarded.\n"
            "- note: additional notes or comments about the award.\n"
            # "- Note: A coach may receive multiple different awards in the same year, and awards are given separately "
            # "within each league. The note field is descriptive and does not affect tuple uniqueness.\n"
        )
    elif schema.lower() == 'combined_shutouts':
        schema_desc = (
            "- Each tuple represents a shutout game record in professional hockey.\n"
            "- year: the season year in which the game occurred.\n"
            "- month: the calendar month of the game.\n"
            "- date: the calendar day of the game.\n"
            "- tmID: identifier of the team achieving the shutout.\n"
            "- oppID: identifier of the opposing team.\n"
            "- R/P: indicates whether the game was a regular-season or playoff game.\n"
            "- IDgoalies1: identifier of the first goalie participating in the shutout.\n"
            "- IDgoalies2: identifier of the second goalie participating in the shutout, if applicable.\n"
        )
    elif schema.lower() == 'teams_post':
        schema_desc = (
            "- Each tuple represents one team's aggregate postseason or playoff performance in a specific hockey season.\n"
            "- year: the season year of the postseason record.\n"
            "- tmID: identifier of the team whose postseason statistics are recorded.\n"
            "- lgID: identifier of the league in which the team played during that season.\n"
            "- G: number of postseason games played by the team.\n"
            "- W: number of postseason games won by the team.\n"
            "- L: number of postseason games lost by the team.\n"
            "- T: number of postseason games tied by the team, if ties were possible in that season or league.\n"
            "- GF: total goals scored by the team in the postseason.\n"
            "- GA: total goals allowed by the team in the postseason.\n"
            "- PIM: total penalty minutes accumulated by the team in the postseason.\n"
            "- BenchMinor: number of bench minor penalties or related bench-minor penalty statistic.\n"
            "- PPG: number of power-play goals scored by the team.\n"
            "- PPC: number of power-play chances or opportunities for the team.\n"
            "- SHA: number of short-handed goals allowed by the team.\n"
            "- PKG: number of goals allowed while the team was penalty killing.\n"
            "- PKC: number of penalty-kill chances or situations faced by the team.\n"
            "- SHF: number of short-handed goals scored by the team.\n"
        )
    elif schema.lower() == 'goalies':
        schema_desc = (
            "- Each tuple represents one goalie’s statistical record for a specific team stint in a specific hockey season.\n"
            "- playerID: identifier of the goalie/player.\n"
            "- year: the season year of the goalie record.\n"
            "- stint: the stint number for the goalie within the same season.\n"
            "- tmID: identifier of the team the goalie played for during this stint.\n"
            "- lgID: identifier of the league.\n"
            "- GP: number of regular-season games played by the goalie.\n"
            "- Min: total regular-season minutes played by the goalie.\n"
            "- W: number of regular-season wins credited to the goalie.\n"
            "- L: number of regular-season losses credited to the goalie.\n"
            "- T/OL: number of regular-season ties or overtime losses.\n"
            "- ENG: number of empty-net goals against, or related empty-net goal statistic.\n"
            "- SHO: number of regular-season shutouts recorded by the goalie.\n"
            "- GA: number of regular-season goals allowed by the goalie.\n"
            "- SA: number of regular-season shots against faced by the goalie.\n"
            "- PostGP: number of postseason/playoff games played by the goalie.\n"
            "- PostMin: total postseason/playoff minutes played by the goalie.\n"
            "- PostW: number of postseason/playoff wins credited to the goalie.\n"
            "- PostL: number of postseason/playoff losses credited to the goalie.\n"
            "- PostT: number of postseason/playoff ties, if applicable.\n"
            "- PostENG: number of postseason/playoff empty-net goals against, or related empty-net goal statistic.\n"
            "- PostSHO: number of postseason/playoff shutouts recorded by the goalie.\n"
            "- PostGA: number of postseason/playoff goals allowed by the goalie.\n"
            "- PostSA: number of postseason/playoff shots against faced by the goalie.\n"
        )
    elif schema.lower() == 'team_splits':
        schema_desc = (
            "- Each tuple represents one team's split performance statistics for a specific hockey season, broken down by home/road games and by month.\n"
            "- year: the season year of the team split record.\n"
            "- tmID: identifier of the team.\n"
            "- lgID: identifier of the league in which the team played.\n"
            "- hW: number of home games won by the team.\n"
            "- hL: number of home games lost by the team.\n"
            "- hT: number of home games tied by the team.\n"
            "- hOTL: number of home overtime losses by the team.\n"
            "- rW: number of road games won by the team.\n"
            "- rL: number of road games lost by the team.\n"
            "- rT: number of road games tied by the team.\n"
            "- rOTL: number of road overtime losses by the team.\n"
            "- SepW: number of games won by the team in September.\n"
            "- SepL: number of games lost by the team in September.\n"
            "- SepT: number of games tied by the team in September.\n"
            "- SepOL: number of overtime losses by the team in September.\n"
            "- OctW: number of games won by the team in October.\n"
            "- OctL: number of games lost by the team in October.\n"
            "- OctT: number of games tied by the team in October.\n"
            "- OctOL: number of overtime losses by the team in October.\n"
            "- NovW: number of games won by the team in November.\n"
            "- NovL: number of games lost by the team in November.\n"
            "- NovT: number of games tied by the team in November.\n"
            "- NovOL: number of overtime losses by the team in November.\n"
            "- DecW: number of games won by the team in December.\n"
            "- DecL: number of games lost by the team in December.\n"
            "- DecT: number of games tied by the team in December.\n"
            "- DecOL: number of overtime losses by the team in December.\n"
            "- JanW: number of games won by the team in January.\n"
            "- JanL: number of games lost by the team in January.\n"
            "- JanT: number of games tied by the team in January.\n"
            "- JanOL: number of overtime losses by the team in January.\n"
            "- FebW: number of games won by the team in February.\n"
            "- FebL: number of games lost by the team in February.\n"
            "- FebT: number of games tied by the team in February.\n"
            "- FebOL: number of overtime losses by the team in February.\n"
            "- MarW: number of games won by the team in March.\n"
            "- MarL: number of games lost by the team in March.\n"
            "- MarT: number of games tied by the team in March.\n"
            "- MarOL: number of overtime losses by the team in March.\n"
            "- AprW: number of games won by the team in April.\n"
            "- AprL: number of games lost by the team in April.\n"
            "- AprT: number of games tied by the team in April.\n"
            "- AprOL: number of overtime losses by the team in April.\n"
        )

    elif schema.lower() == 'abalone':
        schema_desc = (
            "- Each tuple represents one abalone specimen with physical measurements and its ring count.\n"
            "- sex: sex category of the abalone, such as male, female, or infant.\n"
            "- length: longest shell measurement of the abalone.\n"
            "- diameter: shell diameter measured perpendicular to length.\n"
            "- height: shell height of the abalone.\n"
            "- whole_weight: whole weight of the abalone.\n"
            "- shucked_weight: weight of the abalone meat.\n"
            "- viscera_weight: gut weight after bleeding.\n"
            "- shell_weight: shell weight after drying.\n"
            "- rings: number of shell rings, commonly used to estimate age.\n"
        )

    elif schema.lower() == 'breast':
        schema_desc = (
            "- Each tuple represents one breast-cancer cytology sample record.\n"
            "- sample_code_number: identifier of the cytology sample.\n"
            "- clump_thickness: assessment of cell clump thickness.\n"
            "- uniformity_of_cell_size: assessment of how uniform the cell sizes are.\n"
            "- uniformity_of_cell_shape: assessment of how uniform the cell shapes are.\n"
            "- marginal_adhesion: assessment of marginal adhesion in the sample.\n"
            "- single_epithelial_cell_size: assessment of epithelial cell size.\n"
            "- bare_nuclei: number or score of bare nuclei observed.\n"
            "- bland_chromatin: assessment of chromatin texture.\n"
            "- normal_nucleoli: assessment of nucleoli appearance.\n"
            "- mitoses: assessment of mitotic activity.\n"
            "- class: diagnostic class label, such as benign or malignant.\n"
        )

    elif schema.lower() == 'echo':
        schema_desc = (
            "- Each tuple represents one patient echocardiogram record related to heart-attack survival analysis.\n"
            "- survival: survival time after the heart attack, usually measured in months.\n"
            "- still_alive: indicator of whether the patient was still alive at the time of follow-up.\n"
            "- age_at_heart_attack: age of the patient when the heart attack occurred.\n"
            "- pericardial_effusion: indicator of whether pericardial effusion was present.\n"
            "- fractional_shortening: fractional shortening measurement from the echocardiogram.\n"
            "- epss: E-point septal separation measurement.\n"
            "- lvdd: left ventricular end-diastolic dimension.\n"
            "- wall_motion_score: wall-motion score derived from the echocardiogram.\n"
            "- wall_motion_index: wall-motion index derived from wall-motion score.\n"
            "- mult: multiplicative or derived echocardiographic measure.\n"
            "- name: patient or record name field.\n"
            "- group: patient grouping field used in the dataset.\n"
            "- alive_at_1: indicator of whether the patient was alive at one year.\n"
        )

    elif schema.lower() == 'claims':
        schema_desc = (
            "- Each tuple represents one insurance or medical claim record.\n"
            "- claim_id: identifier of the claim record.\n"
            "- member_id: identifier of the insured member or patient.\n"
            "- provider_id: identifier of the healthcare provider.\n"
            "- diagnosis_code: diagnosis code associated with the claim.\n"
            "- procedure_code: procedure or service code associated with the claim.\n"
            "- service_date: date on which the medical service was provided.\n"
            "- received_date: date on which the claim was received.\n"
            "- paid_date: date on which the claim was paid, if applicable.\n"
            "- claim_type: type or category of the claim.\n"
            "- claim_status: processing status of the claim.\n"
            "- billed_amount: amount originally billed for the claim.\n"
            "- allowed_amount: amount allowed after plan or policy rules.\n"
            "- paid_amount: amount actually paid for the claim.\n"
        )

    elif schema.lower() == 'hospital':
        schema_desc = (
            "- Each tuple represents one hospital or healthcare facility record.\n"
            "- provider_number: identifier of the hospital or healthcare provider.\n"
            "- hospital_name: official name of the hospital.\n"
            "- address: street address of the hospital.\n"
            "- city: city where the hospital is located.\n"
            "- state: state where the hospital is located.\n"
            "- zip_code: postal code of the hospital location.\n"
            "- county_name: county where the hospital is located.\n"
            "- phone_number: contact phone number of the hospital.\n"
            "- hospital_type: type or category of the hospital.\n"
            "- hospital_ownership: ownership type of the hospital.\n"
            "- emergency_service: indicator of whether the hospital provides emergency services.\n"
        )

    elif schema.lower() == 'weather':
        schema_desc = (
            "- Each tuple represents one weather observation record at a specific station, date, and time.\n"
            "- station_id: identifier of the weather station.\n"
            "- date: date of the weather observation.\n"
            "- time: time of the weather observation.\n"
            "- latitude: latitude of the observation station.\n"
            "- longitude: longitude of the observation station.\n"
            "- elevation: elevation of the observation station.\n"
            "- temperature: observed air temperature.\n"
            "- dew_point: observed dew point temperature.\n"
            "- humidity: observed relative humidity.\n"
            "- pressure: observed atmospheric pressure.\n"
            "- wind_direction: observed wind direction.\n"
            "- wind_speed: observed wind speed.\n"
            "- visibility: observed visibility distance.\n"
            "- precipitation: observed precipitation amount.\n"
            "- snow_depth: observed snow depth.\n"
            "- cloud_cover: observed cloud cover measurement.\n"
            "- weather_condition: categorical description of the observed weather condition.\n"
            "- quality_flag: quality-control flag or indicator for the observation.\n"
        )

    elif schema.lower() == 'routes':
        schema_desc = (
            "- Each tuple represents one airline route between a source airport and a destination airport.\n"
            "- airline: airline code or name operating the route.\n"
            "- airline_id: identifier of the airline.\n"
            "- source_airport: code of the source airport.\n"
            "- source_airport_id: identifier of the source airport.\n"
            "- destination_airport: code of the destination airport.\n"
            "- destination_airport_id: identifier of the destination airport.\n"
            "- codeshare: indicator of whether the route is operated as a codeshare.\n"
            "- stops: number of stops on the route.\n"
            "- equipment: aircraft equipment codes used on the route.\n"
        )

    elif schema.lower() == 'bridges':
        schema_desc = (
            "- Each tuple represents one bridge record with structural, geographic, and design information.\n"
            "- identif: identifier or name of the bridge.\n"
            "- river: river or waterway associated with the bridge.\n"
            "- location: location code or position of the bridge.\n"
            "- erected: year or period when the bridge was erected.\n"
            "- purpose: main purpose or usage of the bridge.\n"
            "- length: length category or measurement of the bridge.\n"
            "- lanes: number of traffic lanes on the bridge.\n"
            "- clear_g: vertical clearance or clearance-related category.\n"
            "- t_or_d: indicator of through or deck design.\n"
            "- material: main construction material of the bridge.\n"
            "- span: span type or span category of the bridge.\n"
            "- rel_l: relative length category of the bridge.\n"
            "- type: structural type or class of the bridge.\n"
        )

    elif schema.lower() == 'pdbx':
        schema_desc = (
            "- Each tuple represents one Protein Data Bank structure or structure-chain metadata record.\n"
            "- pdb_id: identifier of the PDB structure entry.\n"
            "- chain_id: identifier of the molecular chain within the structure.\n"
            "- molecule_name: name of the molecule or macromolecule.\n"
            "- organism: source organism of the molecule.\n"
            "- taxonomy_id: taxonomy identifier of the source organism.\n"
            "- experimental_method: experimental method used to determine the structure.\n"
            "- resolution: structure resolution, if applicable.\n"
            "- deposition_date: date when the structure was deposited.\n"
            "- release_date: date when the structure was released.\n"
            "- sequence_length: length of the molecule or chain sequence.\n"
            "- structure_title: title or description of the structure entry.\n"
            "- classification: functional or structural classification of the entry.\n"
            "- keywords: keywords associated with the structure entry.\n"
        )

    elif schema.lower() == 'adult':
        schema_desc = (
            "- Each tuple represents one person record used for adult income prediction.\n"
            "- age: age of the person.\n"
            "- workclass: employment or work-class category.\n"
            "- fnlwgt: final sampling weight assigned to the record.\n"
            "- education: highest education level as a categorical value.\n"
            "- education_num: numeric encoding of education level.\n"
            "- marital_status: marital status of the person.\n"
            "- occupation: occupation category of the person.\n"
            "- relationship: household relationship category.\n"
            "- race: race category of the person.\n"
            "- sex: sex category of the person.\n"
            "- capital_gain: capital gain amount.\n"
            "- capital_loss: capital loss amount.\n"
            "- hours_per_week: number of working hours per week.\n"
            "- native_country: native country of the person.\n"
            "- income: income class label.\n"
        )

    elif schema.lower() == 'lineitem':
        schema_desc = (
            "- Each tuple represents one line item within a customer order in the TPC-H benchmark.\n"
            "- l_orderkey: identifier of the order containing this line item.\n"
            "- l_partkey: identifier of the part being ordered.\n"
            "- l_suppkey: identifier of the supplier providing the part.\n"
            "- l_linenumber: line number of this item within the order.\n"
            "- l_quantity: quantity ordered for this line item.\n"
            "- l_extendedprice: extended price of the line item before discount and tax.\n"
            "- l_discount: discount applied to the line item.\n"
            "- l_tax: tax applied to the line item.\n"
            "- l_returnflag: return status flag of the line item.\n"
            "- l_linestatus: processing status of the line item.\n"
            "- l_shipdate: date when the line item was shipped.\n"
            "- l_commitdate: committed shipping date for the line item.\n"
            "- l_receiptdate: date when the line item was received.\n"
            "- l_shipinstruct: shipping instruction for the line item.\n"
            "- l_shipmode: shipping mode used for the line item.\n"
            "- l_comment: free-text comment associated with the line item.\n"
        )

    elif schema.lower() == 'ncvoter':
        schema_desc = (
            "- Each tuple represents one voter registration record from a voter database.\n"
            "- voter_id: identifier of the voter record.\n"
            "- county_id: identifier of the county.\n"
            "- county_desc: name or description of the county.\n"
            "- voter_reg_num: voter registration number.\n"
            "- status_cd: voter status code.\n"
            "- voter_status_desc: textual description of the voter status.\n"
            "- reason_cd: status reason code.\n"
            "- voter_status_reason_desc: textual description of the status reason.\n"
            "- res_street_address: residential street address of the voter.\n"
            "- res_city: residential city of the voter.\n"
            "- res_state: residential state of the voter.\n"
            "- res_zip_code: residential ZIP code of the voter.\n"
            "- mail_addr1: mailing address line of the voter.\n"
            "- mail_city: mailing city of the voter.\n"
            "- mail_state: mailing state of the voter.\n"
            "- mail_zip_code: mailing ZIP code of the voter.\n"
            "- sex_code: sex code recorded for the voter.\n"
            "- race_code: race code recorded for the voter.\n"
            "- ethnic_code: ethnicity code recorded for the voter.\n"
        )
    else:
        raise ValueError(f"Unknown schema name: {schema}")
    return schema_desc
