# Installations

Make sure you have JDK 1.8, and the PostgreSQL server and Neo4j server is running. Then just run the following command to start extraction.

```bash
./gradlew run -PappArgs="['39800000', '40000000']"  # parse postid >= 39800000 AND postid < 40000000
```

## Data Overview

The public available stack overflow is available at [https://archive.org/details/stackexchange](https://archive.org/details/stackexchange)

We convert all xml files (?) into Postgresql database. The database has tables: posts, comments, users, etc.

There are roughly about 47 million posts in a PostgreSQL database, including all topics, such as not Java language and not security related posts.

We would first need to filter out the posts that are java security related.

## Explore the database

We can use the PostgreSQL client tools `psql` to explore the database. Run the following command to connect to the database:

```bash
psql -h localhost -p 5432 -d stackoverflow -U extractor -W
```

The password for this database is `extractor`, the same as the user name.

Then you can use commands such as `\dt`, `\d` to get descriptions of the tables, and execute SQL statements (remember to end a statement with ';').

## Java security related filters

We first use tags as a the first filter, we requires posts to satisfy the following criteria to enter the next step in the processing pipe line:

* at least have two tags from the following the predefine tag set

## process all 47 millions parallely on a cluster

we use port forward to tunnel to the database server:

```bash
ssh -f mschen@newriver1.arc.vt.edu -NL 10002:localhost:10002
ssh -f mschen@newriver1.arc.vt.edu -NL 7474:localhost:7474
```

## store the code snippets back to database

work on the first million [0, 1million), extracted 344 (id=[1,344]) snippets

We create the a snippets table to store all the extracted java security related snippets, using the following SQL statement:

```SQL
CREATE TABLE snippets(
    id serial,
    code VARCHAR(24060),
    postid INT,
    PRIMARY KEY (id),
    FOREIGN KEY (ID) REFERENCES posts(id)
 );
```

### incremental saving results

We use the following SQL to create a table to track the processing process.

```SQL
CREATE TABLE marks(
    processed BOOLEAN,
    tagqualified BOOLEAN,
    securityqualified BOOLEAN,
    postid INT,
    PRIMARY KEY (postid),
    FOREIGN KEY (postid) REFERENCES posts(id)
);
```

Use the following SQL to get unprocessed question posts, for example, within the first 2 million posts:

```SQL
SELECT posts.id, posts.parentid, posts.body, posts.tags
FROM posts INNER JOIN marks ON posts.id=marks.postid
WHERE posts.id < 2000000 AND posts.parentid IS NULL
AND (marks.processed IS NULL OR marks.processed=FALSE);
```

To save the extracted code snippets, we use:

```SQL
INSERT INTO snippets(code, indx, postid)
VALUES($aesc6$"+code+"$aesc6$,"+indx+","+postid+")
ON CONFLICT DO NOTHING;
```
