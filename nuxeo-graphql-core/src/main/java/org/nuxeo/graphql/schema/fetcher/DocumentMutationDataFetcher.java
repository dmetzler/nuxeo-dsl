package org.nuxeo.graphql.schema.fetcher;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.graphql.NuxeoGraphqlContext;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class DocumentMutationDataFetcher extends AbstractDataFetcher implements DataFetcher<DocumentModel> {

    public enum Mode {
        CREATE, UPDATE, DELETE
    }

    private String targetDocType;

    private Mode mode;

    public DocumentMutationDataFetcher(String docType, Mode mode) {
        this.targetDocType = docType;
        this.mode = mode;
    }

    @Override
    public DocumentModel get(DataFetchingEnvironment environment) {
        Map<String, Object> docInputMap = environment.getArgument(targetDocType);

        DocumentModel doc;
        CoreSession session = ((NuxeoGraphqlContext) environment.getContext()).getSession();

        doc = getOrCreateDocument(docInputMap, session);

        SchemaManager sm = Framework.getService(SchemaManager.class);
        DocumentType docType = sm.getDocumentType(targetDocType);
        for (Schema schema : docType.getSchemas()) {
            String schemaName = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix : schema.getName();
            Map<String, Object> dataModelMap = (Map<String, Object>) docInputMap.get(schemaName);
            if (dataModelMap != null) {
                for (Entry<String, Object> entry : dataModelMap.entrySet()) {
                    if (schema.getField(entry.getKey()).getType().isSimpleType()) {
                        doc.setProperty(schema.getName(), entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        switch (mode) {
        case UPDATE:
            return session.saveDocument(doc);
        case DELETE:
            session.removeDocument(doc.getRef());
            return null;
        case CREATE:
            return session.createDocument(doc);

        }
        return null;
    }

    private DocumentModel getOrCreateDocument(Map<String, Object> docInputMap, CoreSession session) {
        String id = (String) docInputMap.get("id");
        String path = (String) docInputMap.get("path");
        String name = (String) docInputMap.get("name");

        switch (mode) {
        case UPDATE:
        case DELETE:
            if (StringUtils.isNotBlank(id)) {
                return session.getDocument(new IdRef(id));
            } else {
                return session.getDocument(new PathRef(path));
            }

        case CREATE:
            return session.createDocumentModel(path, name, targetDocType);
        }
        throw new IllegalArgumentException("Mode is not supported");
    }

}
