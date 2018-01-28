package org.nuxeo.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("nuxeo.graphql")
@RepositoryConfig(init = SampleRepositoryConfig.class, cleanup = Granularity.METHOD)
public class GraphQLServiceTest {

    @Inject
    GraphQLService gql;

    @Inject
    CoreSession session;

    @Test
    public void should_retrieve_graphql_service() throws Exception {
        assertThat(gql).isNotNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_retrieve_document_by_path() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String query = " {document(path:\"/default-domain/workspaces/test\") { id path }}";

        Map<String, Object> result = (Map<String, Object>) gql.query(session, query);
        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertThat(document.get("id")).isEqualTo(doc.getId());
        System.out.println(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_retrieve_document_by_id() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String query = " {document(id:\"" + doc.getId() + "\") { id path }}";
        Map<String, Object> result = (Map<String, Object>) gql.query(session, query);
        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertThat(document.get("path")).isEqualTo("/default-domain/workspaces/test");
    }

    @Test
    public void should_be_able_to_retrieve_simple_schema_props() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String query = " {document(id:\"" + doc.getId() + "\") { id path ...on Workspace { dc { title}}}}";


        Map<String, Map<String,Object>> result =(Map<String, Map<String, Object>>) gql.query(session, query);


        assertThat(result.get("document").get("dc")).isNotNull();


        System.out.println(result);
    }

    @Test
    public void should_be_able_to_retrieve_several_docs() throws Exception {
        String query = " {doc1: document(path:\"/default-domain\") { id path }"
                + "doc2: document(path: \"/default-domain/workspaces/test\") { id path}}";

        Map<String, Object> result = (Map<String, Object>) gql.query(session, query);

        assertThat(result.containsKey("doc1")).isTrue();
        assertThat(result.containsKey("doc2")).isTrue();

    }

    @Test
    public void should_be_able_to_query_docs() throws Exception {
        String nxql = "SELECT * FROM Document";
        String query ="{documents(nxql:\"" + nxql + "\") { path}}";
        Map<String, List<Object>> result = (Map<String, List<Object>>) gql.query(session, query);
        assertThat(result.get("documents")).hasSameSizeAs(session.query(nxql));
    }


    @Test
    public void should_be_able_to_query_children() throws Exception {
        String nxql = "SELECT * FROM WorkspaceRoot";
        String query ="{documents(nxql:\"" + nxql + "\") { path children { id path } }}";
        Map<String, List<Object>> result = (Map<String, List<Object>>) gql.query(session, query);
        assertThat(result.get("documents")).hasSameSizeAs(session.query(nxql));
        System.out.println(result);


    }
}
