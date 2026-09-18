# Room reservation capability

Use `room.search` for availability and `room.reserve` only after a user selects a candidate and supplies all application fields.

The provider adapter converts abstract dates, weekdays, and periods into XTU term values and bitmaps. The model never constructs upstream form fields. A reservation requires deterministic preview, explicit confirmation, one execution attempt, and verification through the current user's reservation list.

