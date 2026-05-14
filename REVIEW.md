# PR Review Instructions

## Important issues

Mark as important if the PR introduces any of the following:

- Incorrect business logic
- Missing authorization checks
- Data leakage involving user or pet records
- Sensitive information in logs
- Unsafe SQL or missing query scoping
- Incorrect timezone handling
- Breaking API changes
- Non-backward-compatible database migration

## Do not report

Do not comment on:

- Formatting issues already handled by lint or formatter
- Naming suggestions unless they cause real confusion
- Generated files
- Lock files
- Minor refactoring preferences

## Backend checks

Always check:

- Controller endpoints validate request DTOs
- Service methods enforce user ownership / permission checks
- Time-related values are stored consistently in UTC
- Exceptions do not expose internal details
- New database columns have sensible constraints or defaults

## Frontend checks

Always check:

- API errors are handled
- Loading and empty states are handled
- User-facing forms validate required fields
- Dates are displayed using the user's timezone
- No private data is stored in localStorage unless necessary

## Output style

- Prioritize blocking issues first.
- Keep comments actionable.
- Avoid nitpicks.
- If there are no blocking issues, say so clearly.