# EchoForge - an LLM message dispatch tool
Course project for advanced software quality and security (quality project)

# What it is:
It allows the user to send messages to a locally hosted large language model. LM Studio was used as the LLM hosting system.  Other LLM providers may work, if they offer OpenAI-like API.

# Limitations:

Everything is in plaintext, no authentication or encryption is done between EchoForge and LM Studio because LM Studio does not offer such niceties yet.



# Requirements:

Java 22, everything mentioned in pom.xml. Run `mvn clean package` to get an executable jar file.
