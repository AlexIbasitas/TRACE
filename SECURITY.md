# Security Policy

## API Key Storage

API keys are stored securely using IntelliJ Platform's PasswordSafe, which provides encrypted local storage. Keys are never transmitted in plain text or stored in configuration files. Users maintain full control over their credentials and can revoke access at any time through their respective service providers.

## Network Security

All network communications use HTTPS encryption exclusively. The plugin only connects to:
- OpenAI API (api.openai.com)
- Google Gemini API (generativelanguage.googleapis.com)

No other network endpoints are accessed. All requests are authenticated using industry-standard API key authentication.

## Data Handling

The plugin processes test failure context locally within the IDE. No personal data, source code, or sensitive information is collected, stored, or transmitted. Test failure analysis is performed using only:
- Stack traces
- Step definitions
- Test scenarios
- Error messages

All data processing occurs locally in the user's IDE environment.

## Privacy

This plugin does not collect telemetry, usage statistics, or any form of user data. Test failure analysis data is sent only to the AI services (OpenAI and Google Gemini) that users explicitly configure. No information is sent to third-party analytics services or stored on external servers beyond the configured AI providers. Users retain complete control over their data and development environment.

## Vulnerability Reporting

Security vulnerabilities should be reported privately to: aibasitas1@gmail.com

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact assessment
- Suggested remediation (if applicable)

## Updates

Keep the plugin updated to the latest version to ensure you have the most recent security improvements and bug fixes.
