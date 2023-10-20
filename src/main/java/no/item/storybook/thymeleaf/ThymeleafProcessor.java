package no.item.storybook.thymeleaf;

import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceProblemException;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.trace.Trace;
import com.enonic.xp.trace.Tracer;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.apache.commons.io.FilenameUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

public final class ThymeleafProcessor
{
    private final TemplateEngine engine;

    private ResourceKey view;

    private final Map<String, Object> parameters;

    private TemplateMode mode;

    public ThymeleafProcessor(final TemplateEngine engine, final ThymeleafViewFunctions viewFunctions )
    {
        this.engine = engine;
        this.parameters = Maps.newHashMap();

        this.parameters.put( "portal", viewFunctions );
    }

    public void setView( final ResourceKey view )
    {
        this.view = view;
    }

    public void setModel( final ScriptValue model )
    {
        if ( model != null )
        {
            this.parameters.putAll( model.getMap() );
        }
    }

    public void setMode( final String mode )
    {
        try
        {
            this.mode = TemplateMode.valueOf( mode.toUpperCase() );
        }
        catch ( final Exception e )
        {
            this.mode = TemplateMode.HTML;
        }
    }

    public String process()
    {
        final Trace trace = Tracer.newTrace( "thymeleaf.render" );
        if ( trace == null )
        {
            return doProcess();
        }

        return Tracer.trace( trace, () -> {
            trace.put( "path", this.view.getPath() );
            trace.put( "app", this.view.getApplicationKey().toString() );
            return doProcess();
        } );
    }

    private String doProcess()
    {
        try
        {
            final Context context = new Context();
            context.setVariables( this.parameters );

            final TemplateSpec spec = new TemplateSpec( this.view.toString(), this.mode );
            return this.engine.process( spec, context );
        }
        catch ( final RuntimeException e )
        {
            throw handleException( e );
        }
    }

    private RuntimeException handleException( final RuntimeException e )
    {
        if ( e instanceof TemplateProcessingException )
        {
            return handleException( (TemplateProcessingException) e );
        }

        return e;
    }

    private RuntimeException handleException( final TemplateProcessingException e )
    {
        final int lineNumber = e.getLine() != null ? e.getLine() : 0;
        final ResourceKey resource = getTemplateResource( e.getTemplateName() );
        String message = e.getMessage();
        if ( resource == null )
        {
            message = "Error in Thymeleaf template '" + e.getTemplateName() + "' : " + e.getMessage();
        }

        return ResourceProblemException.create().
            lineNumber( lineNumber ).
            resource( resource ).
            cause( e ).
            message( message ).
            build();
    }

    private ResourceKey getTemplateResource( final String templateName )
    {
        if ( templateName == null )
        {
            return null;
        }

        try
        {
            return ResourceKey.from( templateName );
        }
        catch ( final Exception e )
        {
            try
            {
                return ResourceKey.from( this.view.getApplicationKey(), resolveTemplatePath( templateName ) );
            }
            catch ( Exception ex )
            {
                return null;
            }
        }
    }

    private String resolveTemplatePath( final String templateName )
    {
        String path = templateName;
        if ( FilenameUtils.getExtension( templateName ).isEmpty() )
        {
            path += ".html";
        }

        if ( templateName.startsWith( "./" ) || templateName.startsWith( "../" ) )
        {
            return Files.simplifyPath( this.view.getPath() + "/../" + path );
        }
        return path;
    }
}
