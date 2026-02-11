# SAP Commerce Skill

A comprehensive AI coding agent skill for SAP Commerce Cloud (Hybris) development. Provides guidance, templates, and utilities for building e-commerce solutions.

Works with **35+ AI coding agents** including Claude Code, Cursor, Cline, GitHub Copilot, Windsurf, Codex, and more.

## Installation

```bash
npx skills add Emenowicz/sap-commerce-skill
```

The CLI auto-detects which agents you have installed and configures the skill for all of them.

### Supported Agents

Claude Code, Cursor, Cline, GitHub Copilot, Codex, Continue, Windsurf, Aider, Gemini, OpenCode, Augment, Zed, Kilo Code, Roo Code, Trae, and [many more](https://skills.sh).

## What's Included

### Reference Documentation
- **Type System** - items.xml, attributes, relations, enumerations
- **Service Layer** - Facade/Service/DAO patterns, Spring DI
- **Extension Development** - Structure, configuration, Cloud vs On-Premise
- **ImpEx Guide** - Data import/export scripting
- **FlexibleSearch** - Query syntax, joins, performance
- **OCC API** - REST endpoints, DTOs, authentication
- **Accelerators** - B2C/B2B customization, checkout flows
- **Spring Configuration** - Beans, AOP, events
- **Data Modeling** - Products, orders, users, pricing
- **Troubleshooting** - Common errors and solutions

### Code Templates
- Complete extension boilerplate
- Service layer (Facade/Service/DAO/DTO)
- Item type definitions
- ImpEx import scripts
- FlexibleSearch queries
- OCC REST API customization
- Checkout flow extension

### Utility Scripts
- `generate-extension.sh` - Scaffold new extensions
- `validate-impex.sh` - Validate ImpEx syntax
- `query-items.sh` - Execute FlexibleSearch via HAC

## Coverage

| Aspect | Support |
|--------|---------|
| Deployment | Cloud (CCv2), On-Premise |
| Project Types | B2C, B2B |
| Integrations | Payment, ERP/SAP, Solr, CMS |

## Usage

Once installed, your agent automatically activates this skill when you:

- Ask to create SAP Commerce extensions
- Need to define item types
- Want to write ImpEx scripts
- Implement service layer components
- Customize OCC REST APIs
- Work with FlexibleSearch queries
- Extend B2C/B2B accelerators

### Examples

```
> Create a custom extension for loyalty points management

> Write ImpEx to import products with prices and stock levels

> Implement a service layer for customer wishlists

> How do I add a custom checkout step?
```

## Structure

```
sap-commerce/
├── SKILL.md              # Skill definition
├── references/           # 10 detailed guides
├── assets/               # 37 code templates
└── scripts/              # 3 utility scripts
```

## License

MIT License - see [LICENSE](LICENSE)

## Contributing

Contributions welcome! Please open an issue or submit a PR.

## Related

- [skills.sh](https://skills.sh) - Skills directory
- [SAP Commerce Cloud Help](https://help.sap.com/docs/SAP_COMMERCE_CLOUD)
