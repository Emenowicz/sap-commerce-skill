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
- **CronJobs & Task Engine** - Scheduled jobs, triggers, task runners
- **Business Processes** - Order flows, return processes, workflow actions
- **Solr Search** - Indexed types, facets, value providers, boost rules
- **Promotions & Rule Engine** - Drools rules, coupons, custom actions
- **Caching** - Region caches, Spring cache, cluster invalidation
- **Backoffice** - Widget config, editors, search, wizards

### Code Templates
- Complete extension boilerplate
- Service layer (Facade/Service/DAO/DTO)
- Item type definitions
- ImpEx import scripts
- FlexibleSearch queries
- OCC REST API customization
- Checkout flow extension
- CronJob implementation and configuration
- Business process definitions and actions
- Solr search setup and custom value providers
- Promotion rules and coupon setup

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
| Background Jobs | CronJobs, Task Engine |
| Workflows | Business Processes, Order/Return Flows |
| Search | Solr Configuration, Facets, Indexing |
| Marketing | Promotions, Coupons, Rule Engine |
| Performance | Caching, Cache Regions, Monitoring |
| Admin UI | Backoffice Configuration, Widgets |

## Usage

Once installed, your agent automatically activates this skill when you:

- Ask to create SAP Commerce extensions
- Need to define item types
- Want to write ImpEx scripts
- Implement service layer components
- Customize OCC REST APIs
- Work with FlexibleSearch queries
- Extend B2C/B2B accelerators
- Create CronJobs or scheduled tasks
- Define business processes or order flows
- Configure Solr search and indexing
- Set up promotions and coupons
- Customize Backoffice admin UI

### Examples

```
> Create a custom extension for loyalty points management

> Write ImpEx to import products with prices and stock levels

> Implement a service layer for customer wishlists

> How do I add a custom checkout step?

> Create a CronJob that exports order data nightly

> Configure Solr faceted search for product categories
```

## Structure

```
sap-commerce/
├── SKILL.md              # Skill definition
├── references/           # 16 detailed guides
├── assets/               # 51 code templates
└── scripts/              # 3 utility scripts
```

## License

MIT License - see [LICENSE](LICENSE)

## Contributing

Contributions welcome! Please open an issue or submit a PR.

## Related

- [skills.sh](https://skills.sh) - Skills directory
- [SAP Commerce Cloud Help](https://help.sap.com/docs/SAP_COMMERCE_CLOUD)
