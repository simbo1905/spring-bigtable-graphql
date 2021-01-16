
# A Demo Of GraphQL-Java Over bigtable deployed onto KNative on GKE

This codebase is based on the tutorial [getting-started-with-spring-boot](https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/).

Exactly like the original tutorial if we query with:

```graphql
{
  bookById(id:"book-1"){
    id
    name
    pageCount
    author {
      firstName
      lastName
    }
  }
}
```

Then we get back: 

```json
{
  "data": {
    "book1": {
      "id": "book-1",
      "name": "Harry Potter and the Philosopher's Stone",
      "pageCount": 223,
      "author": {
        "firstName": "Joanne",
        "lastName": "Rowling"
      }
    }
  }
}
```

You can run GraphQL Playbround to try this out: 

![GraphQL Playground](https://raw.githubusercontent.com/simbo1905/bigtable-graphql/master/graphql-bigtable.png)

The GraphQL scheme is:

```graphql
type Query {
    bookById(id: ID): Book
}

type Book {
    id: ID
    name: String
    pageCount: Int
    author: Author
}

type Author {
    id: ID
    firstName: String
    lastName: String
}
```

The matching bigtable schema is:

```sql
create table demo_graphql_java.book ( id string, name string, pageCount string, authorId string ); 
create table demo_graphql_java.author ( id string, firstName string, lastName string );  
```

This codebase uses a generic file to `wirings.json` to map GraphQL onto bigtable SQL. If we look in that file we have:

```json
[
  {
    "typeName": "Query",
    "fieldName": "bookById",
    "sql":"select id,name,pageCount,authorId from demo_graphql_java.book where id=@id",
    "mapperCsv":"id,name,pageCount,authorId",
    "gqlAttr": "id",
    "sqlParam": "id"
  },
  {
    "typeName": "Book",
    "fieldName": "author",
    "sql":"select id,firstName,lastName from demo_graphql_java.author where id=@id",
    "mapperCsv":"id,firstName,lastName",
    "gqlAttr": "authorId",
    "sqlParam": "id"
  }
]
```

That contains two wiring: 

 1. There is a field on `Query` called `bookById` which fines our top level query:
    * The graphql source parameter/attribute is `id` as we can query by e.g., `bookById(id:"book-1")`
    * The sql query named parameter is also `id` as that is the identity column on the book table. 
    * The sql query is a simple select-by-id that uses the sql param i.e., `where id=@id`
    * The list of fields returned by the query is named in `mapperCsv`. We have to pass this as bigtable won't tell us this fact unlike a standard JDBC ResultSet :cry:.
 2. There is a field on `Book` called `author` which requires querying the author table based on the `authorId` of the book:
    * The graphql source parameter/attribute is `authorId` as this is the name of the attribute on the `Book` entity as loaded from bigtable.
    * The sql query named parameter is `id` as that is also the column name of the identity column on the author table.
    * The sql query is also a simple select-by-id using `where id=@id`
    * Once again the list of the fields returned by the query is supplied as bigtable doesn't provide that :cry:.

## TODO Development

At the moment the code assumes that all SQL query parameters are strings. 
It is left as an exercise to the reader to upgrade the code to deal with other types. 

## BigTable Setup

On the Google Cloud console: 

 1. Create the BigTable cluster. 
 2. Create a service account `bigtable-graphql` and grant it bigtable admin perissions
 3. Set the cluster details in `application.properties` then run the main method in BigTableInitializer

Note in 2020.3 IntelliJ it is refusing to pickup `application.properties` that file as [IDEA-221673](https://youtrack.jetbrains.com/issue/IDEA-221673?_ga=2.261730190.2065449588.1610823467-1536685944.1605418802)


