# MetaManager
Meta management for Elasticsearch

Project includes an executable JAR file that accepts indexs, aliases and type definitions from an Excel or Open Office workbook and updates Elasticsearch.  Features include:

  All definitions made via an Excel or Open Office workbook, with edit validity checking.
  Allows types and associated fields to be defined using the available Elasticsearch settings.
  Types support single inheritance and Sub/Nested Types.
  Indexes can be versioned (almost), allowing field and type changes to be migrated from one version of the index to the next 	  using a common alias. Rivers can be defined and executed after the index build process with timeout settings river testing.
  Multiple JUnit tests can be triggered following the index/river building process.
  
Current Support/Limitations:
  ES 1.4 core types supported except norms, index options and token count.  Nested, ip and geopoint supported.
	Fielddata filters, similarity not supported.  Limited support for type and index options. 
	Excel edits are mostly complete.
	A common alias is required for each index, with the expectation that index names are versioned.
	Index versioning not complete. River timeouts not complete. 
	
