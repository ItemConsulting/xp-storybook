package no.item.storybook.thymeleaf;

import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.view.ViewFunctionService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.google.common.collect.Sets;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.standard.StandardDialect;
import java.util.Set;

public final class ThymeleafService
    implements ScriptBean
{
    private final TemplateEngine engine;

    private BeanContext context;

    public ThymeleafService()
    {
        this.engine = new TemplateEngine();

        final Set<IDialect> dialects = Sets.newHashSet();
        dialects.add( new ExtensionDialectImpl() );
        dialects.add( new StandardDialect() );

        this.engine.setDialects( dialects );
    }

    public ThymeleafFileProcessor newFileProcessor(String baseDirPath)
    {
        return new ThymeleafFileProcessor(getTemplateEngine(baseDirPath), createViewFunctions(baseDirPath));
    }

    public ThymeleafInlineProcessor newInlineTemplateProcessor(String baseDirPath)
    {
        return new ThymeleafInlineProcessor(getTemplateEngine(baseDirPath), createViewFunctions(baseDirPath));
    }

    private ThymeleafViewFunctions createViewFunctions(String baseDirPath)
    {
        final ThymeleafViewFunctions functions = new ThymeleafViewFunctions(baseDirPath);
        functions.viewFunctionService = this.context.getService( ViewFunctionService.class ).get();
        functions.portalRequest = PortalRequestAccessor.get();
        return functions;
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.context = context;
    }

    private TemplateEngine getTemplateEngine(String baseDirPath) {
        if(!engine.isInitialized()) {
            StorybookTemplateResolver resolver = new StorybookTemplateResolver();
            resolver.setPrefix(baseDirPath);
            resolver.setSuffix(".html");

            this.engine.setTemplateResolver(resolver);
        }

        return engine;
    }
}
