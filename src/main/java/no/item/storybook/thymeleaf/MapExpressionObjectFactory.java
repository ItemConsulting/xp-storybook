package no.item.storybook.thymeleaf;

import com.google.common.collect.Maps;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Map;
import java.util.Set;

final class MapExpressionObjectFactory
    implements IExpressionObjectFactory
{
    private final Map<String, Object> map;

    public MapExpressionObjectFactory()
    {
        this.map = Maps.newHashMap();
    }

    @Override
    public Set<String> getAllExpressionObjectNames()
    {
        return this.map.keySet();
    }

    @Override
    public Object buildObject( final IExpressionContext context, final String name )
    {
        return this.map.get( name );
    }

    @Override
    public boolean isCacheable( final String name )
    {
        return true;
    }

    public void put( final String name, final Object value )
    {
        this.map.put( name, value );
    }
}
