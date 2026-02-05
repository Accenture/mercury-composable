# Resources files for the test branch

Config files in the test/resources folder are copied from the main/resources folder to ensure unit tests
run with predictable mock data.

This allows you to update the main app and configuration to test drive the behavior.
The unit tests will run fine unless you made breaking changes to the source code.

# Preparation for production

Before going for production, please move the mock services from "main" to "test" folder.

Please update your application.properties in "main" to externalize the configuration files to a transient file location
populated by your deployment pipeline.

This is a sample framework application. You must implement the necessary protocol skills such as OAuth 2.0
authentication that are relevant to your organization's requirements.

Usually the number of protocol skills is small so it should not take too much time to productize your deployment.
Update the provider specific protocol to point to your new services that offer the required protocol skills.

```properties
#
# data dictionary folder
#
location.data.dictionary=classpath:/dictionary
location.data.provider=classpath:/providers
location.questions=classpath:/questions
#
# Data Providers
# (assign service route names to providers)
#
provider.mdm=simple.http.service
provider.account=simple.http.service

```