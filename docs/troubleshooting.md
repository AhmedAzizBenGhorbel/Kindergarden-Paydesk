# Troubleshooting

Use a local development database and avoid placing real payment or child information in diagnostic captures. The application activity log records user actions; it is not a diagnostic log. For connection failures, inspect the Java exception and the MySQL server log locally, and redact usernames, paths, and connection details before sharing output.

| Symptom | Check | Action |
| --- | --- | --- |
| Connection refused or communications failure | Confirm MySQL is running and listening at the configured host and port. Check that the server is reachable from this machine. | Start the local MySQL service or correct the local connection settings, then restart the application. |
| Access denied | Check which database account is configured and whether it has privileges on the application database. | Correct the local account or grant only the required database permissions. Do not paste a password into a command, issue, or screenshot. |
| Unknown database, missing table, or missing column | Check that the intended database exists and whether the explicit initializer completed. Compare the database with `src/main/resources/database/schema.sql`. | The initializer also reruns `seed.sql`: duplicate-key clauses reactivate/reset the seeded admin role, restore school-year/month defaults, and overwrite the garderie name, deadline, and backup-folder settings. Run it only for fresh/disposable setup or after a reviewed backup and state-impact check; do not use it routinely as a repair command on a working database. |
| `mysqldump` cannot be started | Check that the MySQL client tools are installed and `mysqldump` is available to the application. | Install the matching MySQL client tools or add their `bin` directory to the process PATH, then retry to a writable backup folder. |
| Dump reports an error or creates no usable file | Check the backup attempt status/message, output file existence/size, and database privileges. | Resolve the reported command or permission issue and retry. Do not treat a recorded backup attempt as proof of recoverability; perform the isolated restore exercise. |

Never use the live or normal development database as a restore target for a recovery check. See [Restore exercise](restore-exercise.md) for a disposable target workflow.
