# multitenant-connection-provider

[![Join the chat at https://gitter.im/tuxetuxe/multitenant-connection-provider](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tuxetuxe/multitenant-connection-provider?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
This is a very simple (and naive ? :) ) way to kickstart the usage of the Hibernate multi 
tenancy feature.

## How to use
* Add this artifact to your project
* Extend the class com.twimba.hibernate.AbstractMultiTenantConnectionProvider
* Implement a org.hibernate.context.spi.CurrentTenantIdentifierResolver 
* Configure hibernate to use multi tenancy:
```
		params.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name());
		params.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, <a sub class of AbstractMultiTenantConnectionProvider>);
		params.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, <an implementation of the CurrentTenantIdentifierResolver> );
```
* Add tenants configuration.
* Done! :)

There is a sample implementation in [here](https://github.com/tuxetuxe/multitenant-connection-provider-sample)

#### References: 
* https://hibernate.atlassian.net/browse/HHH-7582
* https://github.com/hibernate/hibernate-orm/pull/636

## License
This is available in the Apache Licence 2.0
http://www.tldrlegal.com/license/apache-license-2.0-(apache-2.0)
