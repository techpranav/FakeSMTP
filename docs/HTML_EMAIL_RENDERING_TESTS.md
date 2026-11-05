# HTML email rendering tests

## Rules

- MailBox chrome themes must not rewrite received HTML.
- No application CSS injection into email bodies.
- HTML viewport uses independent white `EMAIL_SURFACE`.
- Flying Saucer / preprocessor may sanitize scripts and block remote images per existing policy; they must not invert colors or strip enterprise table styling.

## Fixtures

| Fixture | Path | Covers |
|---------|------|--------|
| Overdue approval | `src/test/resources/emails/overdue.html` | Long enterprise content, multi-column tables, hyperlinks, SVG charts, special characters, remote image blocked |
| Wide table | `src/test/resources/emails/wide-table.html` | Wide multi-column table, unicode, SVG, hyperlinks, mixed sections |

## Automated coverage

- `EmailRenderTest.rendersTheOverdueApprovalEmail`
- `EmailRenderTest.rendersWideTableFixtureWithoutThemeInjection`
- `EmailRenderTest.overdueApprovalRemainsAccessibleRegressionFixture`
- `EmailRenderTest.resolvesCidAndBlocksRemoteImages`

## Manual packaged JAR checks

1. Inject overdue-approval HTML over SMTP and open it in the reader (dark chrome, white table surface, SVG bars, hyperlinks).
2. Confirm white email surface in Dark and Light themes.
3. Confirm table columns remain readable; the application theme does not restyle table borders.
4. Confirm hyperlinks remain present (SafeLinks policy for activation).
5. Confirm SVG/chart bars remain visible when embedded.
6. Confirm wide-table fixture remains scrollable without theme CSS bleed.
7. Automated raster: `target/overdue-approval.png` copied to `docs/images/overdue-approval.png`.

## Theme isolation

Dark/Light chrome colors must never appear as injected styles inside `preparedHtml`. Regression asserted in `EmailRenderTest` (`mailbox-theme` absent).
