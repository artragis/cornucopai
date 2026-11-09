## Scenario: Brian's Discovery

Imagine a situation where Brian learns about your system's configurations through error
messages or misconfigurations on your website.

### Threat Modeling

The situation falls under the **Information Disclosure** category in STRIDE.

### What can go wrong?

Verbose error messages, default files and exposed source code give attackers extra
visibility into the system's inner workings.

### What are we going to do about it?

1. Implement generic, non-revealing error messages.
2. Ensure system configurations are secure and default installation files are removed.
