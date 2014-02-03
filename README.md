# multitenant-connection-provider
This is a very simple (and naive ? :) ) way to kickstart the usage of the Hibernate multi 
tenancy feature.

## How to use
Add this artifact to your project
Extend the class com.twimba.hibernate.AbstractMultiTenantConnectionProvider
Implement a org.hibernate.context.spi.CurrentTenantIdentifierResolver 
Configure hibernate to use multi tenancy:
```
		params.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name());
		params.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, <a sub class of this>);
		params.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, <an implementation of the CurrentTenantIdentifierResolver> );
```
Add tenants configuration.
Done! :)

## PitFalls
This code assumes you are using hibernate 4.3.0_Final. It is possible to use with previous 
versions (tested from 4.2.7_Final) however be aware that before 4.3.0 there is a bug with 
the ID generators not taking into account the tenant!
References: 
	https://hibernate.atlassian.net/browse/HHH-7582
	https://github.com/hibernate/hibernate-orm/pull/636

## License
This is available in the Apache Licence 2.0
http://www.tldrlegal.com/license/apache-license-2.0-(apache-2.0)
