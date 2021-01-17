
# A Demo Of GraphQL-Java over Bigtable deployed onto KNative on GKE

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

![GraphQL Playground](https://raw.githubusercontent.com/simbo1905/bigquery-graphql/master/graphql-bigquery.png)

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

This codebase uses a generic file to `wirings.json` to map GraphQL onto Bigtable. If we look in that file we have:

```json
[
  {
    "typeName": "Query",
    "fieldName": "bookById",
    "gqlAttr": "id",
    "table": "book",
    "family": "entity",
    "qualifiesCsv":"id,name,pageCount,authorId"
  },
  {
    "typeName": "Book",
    "fieldName": "author",
    "gqlAttr": "authorId",
    "table": "author",
    "family": "entity",
    "qualifiesCsv":"id,firstName,lastName"
  }
]
```

That contains two wiring: 

 1. There is an object `Query`
    * With a field `bookById`
    * Where the row key will be passed as `id`
    * The table to query is `book`
    * The column family to query is `entity`
    * The column qualifiers to pull back are `id,name,pageCount,authorId`
 2. There is a object on `Book`
    * With a field `author`
    * Where the row key will be passed as `authorId`
    * The table to query is `author`
    * The column family to query is `entity`
    * The column qualifiers to pull back are `id,firstName,lastName`

## Bigtable Setup

On the Google Cloud console: 

 1. Create the Bigtable cluster. 
 2. Create a service account `bigtable-graphql` and grant it Bigtable admin perissions
 3. Set the cluster details in `application.properties`
 4. Then run the main method in `com.github.simbo1905.bigtablegraphql.BigtableInitializer`

That should create the tables and populate them with the values from the original demo. 

## Known Issues

Debugging in IntelliJ in version 2020.3 it is refusing to pickup `application.properties` that file as [IDEA-221673](https://youtrack.jetbrains.com/issue/IDEA-221673?_ga=2.261730190.2065449588.1610823467-1536685944.1605418802). So to debug the code you need to hardcode your project details into the code. :unamused:

## Run On KNative

Do the first helloworld deployment from the video [Serverless with Knative - Mete Atamel](https://www.youtube.com/watch?v=HiIJqMqFbC0).
**Note** The latest setup material is at [https://github.com/meteatamel/knative-tutorial/tree/master/setup](https://github.com/meteatamel/knative-tutorial/tree/master/setup) and you *only* need to setup Knative Serving and not anything else.

We need to create a secret containing your service account token.

Using [these instructions](https://knative.dev/docs/serving/samples/secrets-go/) I found that this worked:

```sh
kubectl create secret generic graphql-bigtable-secret --from-file=bigtable-sa.json
```

Grab the latest helm and put it on your path. Then install the KNative service with:

```sh
helm install bigtable-graphql ./bigtable-graphql
```

you can view it and uninstall it with:

```sh
helm list
helm uninstall bigtable-graphl
```

Better yet use [github pages](https://docs.github.com/en/free-pro-team@latest/github/working-with-github-pages/creating-a-github-pages-site) and create a chart repo:

```sh
helm package bigtable-graphql
mv bigtable-graphql-1.0.3.tgz charts/
helm repo index charts --url https://${YOUR_ORG}.github.io/bigtable-graphql/charts
git add charts/*
git commit -am 'charts update'
git pull && git push
```

Now you can use the declarative helmfile.yaml to update all the services with:

```sh
helmfile sync
```
