# SAP Commerce 2211 JDK 21 Baseline

## Target

This skill targets SAP Commerce Cloud `2211-jdk21.1` or later. For CCv2, use the rolling JDK 21 line:

```json
{
  "commerceSuiteVersion": "2211-jdk21"
}
```

SAP introduced the JDK 21 framework update in `2211-jdk21.1`. The update includes JDK 21 and Spring 6. Always review the release notes for the exact `2211-jdk21.x` build selected by the Cloud Portal.

## Code Rules

- Compile and run custom extensions with SapMachine/OpenJDK 21.
- Use Spring 6-compatible APIs and dependencies.
- Use `jakarta.annotation.*` and `jakarta.validation.*`; do not add new `javax.annotation` or `javax.validation` imports.
- Use OpenAPI 3 annotations from `io.swagger.v3.oas.annotations`; do not use SpringFox Swagger 2 annotations.
- Keep third-party libraries compatible with Java 21 and Spring 6.
- Configure OCC CORS through `corsfilter.<web-extension>.*` properties, CCv2 manifest properties, or ImpEx.
- Treat JSP Accelerator examples as legacy migration material, not the default for new storefronts.

## Upgrade Checklist

1. Set `commerceSuiteVersion` to `2211-jdk21`.
2. Build custom extensions locally with JDK 21.
3. Replace application-facing `javax.annotation`, `javax.validation`, and `javax.servlet` imports with Jakarta equivalents.
4. Review custom web extensions for Tomcat 10 and Spring 6 compatibility. Do not recreate platform MVC infrastructure with `<mvc:annotation-driven>` inside OCC web contexts.
5. Upgrade or remove libraries that do not support Java 21 or Spring 6, including storefront dependencies such as JSTL and wro4j where used.
6. Replace Swagger 2/SpringFox annotations with OpenAPI 3 annotations.
7. Review removed extensions and the OAuth and Drools changes documented for `2211-jdk21.1`.
8. Run `ant clean all`, unit tests, integration tests, and an initialization/update rehearsal against a disposable environment.
9. Validate OCC authentication and authorization, CORS, Backoffice, Solr indexing, CronJobs, business processes, and promotion rules before promotion.

## Official SAP References

- [Update Release 2211-jdk21.1](https://help.sap.com/docs/SAP_COMMERCE_CLOUD_PUBLIC_CLOUD/251a1313585a498eb998c2f974f4f338/114bc6baf83c427289abacdb2db36a9b.html)
- [Specifying SAP Commerce Cloud Update Version](https://help.sap.com/docs/SAP_COMMERCE_CLOUD_PUBLIC_CLOUD)
