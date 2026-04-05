package back.domain.prompt.chunking.service;

import java.util.List;

public interface EmbeddingService {

    public List<Float> embed(String text);

    public List<List<Float>> embedBatch(List<String> texts);
}
