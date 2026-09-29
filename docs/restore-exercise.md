# Isolated restore exercise

This is a proposed recovery exercise. It has not been run as part of the documentation work. Run it only with a SQL dump and a local disposable MySQL instance. The target database must be separate from the application's configured database and contain no data you need.

1. Create a uniquely named, empty scratch database (for example, `paydesk_restore_lab_20260929`) on a local MySQL instance. Confirm the selected host and database before continuing.
2. Restore the dump into that scratch database using the MySQL client. Because this project's dump may include database-selection statements, inspect the SQL file first; if it names the normal application database, edit a copy for the lab or use a dedicated disposable MySQL instance. Never execute such a dump against the normal application database.
3. Check that expected tables exist, including `users`, `children`, `monthly_records`, `payment_entries`, and `receipts`. Confirm basic schema properties such as primary keys and foreign keys.
4. Query counts from representative tables and inspect a few synthetic records. Compare the results with the source database's known synthetic fixture data, without exposing personal data.
5. Record the dump filename, client/server versions, the disposable target name, checks performed, and any failure. A successful import is evidence of a tested recovery path for that dump; it is not a guarantee for every backup or environment.
6. Remove the scratch database only after confirming its exact name and that it is disposable.

Keep the exercise separate from the app's selected database. This repository's backup feature creates a file; backup creation and restore verification are distinct operational checks.
