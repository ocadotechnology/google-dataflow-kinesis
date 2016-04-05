/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.bigquery.model.*;

import java.io.IOException;
import java.util.List;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

/**
 * Created by ppastuszka on 14.12.15.
 */
public class BQ {
    private final Bigquery bigquery;

    BQ() {
        try {
            JacksonFactory jaksonFactory = JacksonFactory.getDefaultInstance();
            NetHttpTransport httpTransport = newTrustedTransport();
            GoogleCredential credential = GoogleCredential.getApplicationDefault
                    (httpTransport, jaksonFactory).createScoped(BigqueryScopes.all());
            this.bigquery = new Bigquery.Builder(httpTransport, jaksonFactory, credential).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BQ get() {
        return Holder.INSTANCE;
    }

    public static void main(String[] args) throws IOException {
        List<String> rows = get().readAllFrom(
                new TableReference().setProjectId(args[0]).setDatasetId(args[1]).setTableId(args[2])
        );
        System.out.println(rows.size());
    }

    public Table createTable(TableReference reference, TableSchema tableSchema) throws IOException {
        return bigquery.tables().insert(
                reference.getProjectId(),
                reference.getDatasetId(),
                new Table().setTableReference(reference).setSchema(tableSchema)
        ).execute();
    }

    public void deleteTableIfExists(TableReference reference) throws IOException {
        try {
            bigquery.tables().delete(
                    reference.getProjectId(),
                    reference.getDatasetId(),
                    reference.getTableId()).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().getCode() != 404) {
                throw e;
            }
        }
    }

    public List<String> readAllFrom(TableReference reference) throws IOException {
        String query = String.format("SELECT * FROM %s.%s", reference.getDatasetId(),
                reference.getTableId());
        QueryResponse response = bigquery.jobs().query
                (reference.getProjectId(), new QueryRequest().setQuery(query)).execute();

        List<TableRow> rows = Lists.newArrayList();
        if (response.getRows() != null) {
            rows.addAll(response.getRows());
        }
        String pageToken = response.getPageToken();

        while (pageToken != null) {
            GetQueryResultsResponse rowsResponse =
                    bigquery.jobs().getQueryResults(reference.getProjectId(), response
                            .getJobReference().getJobId()).setPageToken(pageToken).execute();

            pageToken = rowsResponse.getPageToken();

            if (rowsResponse.getRows() != null) {
                rows.addAll(rowsResponse.getRows());
            }
        }

        List<String> columnValues = Lists.newArrayList();
        for (TableRow row : rows) {
            columnValues.add((String) row.getF().get(0).getV());
        }
        return columnValues;
    }

    private static class Holder {
        private static final BQ INSTANCE = new BQ();
    }
}