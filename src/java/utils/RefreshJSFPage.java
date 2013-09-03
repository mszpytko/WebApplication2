/*
 * Refresh JSF page programmatically from JavaBean
 * When business logic in JavaBean decides that forced jsf page 
 * refresh is needed, it can be done from logic’s level.
 */
package utils;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author michal
 */
public class RefreshJSFPage {

    public void refresh() {
        FacesContext context = FacesContext.getCurrentInstance();
        String viewId = context.getViewRoot().getViewId();
        ViewHandler handler = context.getApplication().getViewHandler();
        UIViewRoot root = handler.createView(context, viewId);
        root.setViewId(viewId);
        context.setViewRoot(root);
        System.out.println("RefreshJSFPage: refresh viewId="+viewId+" root="+root.toString());
    }
    /* Browsers may have cache enabled, so some cached elements 
     * may not be refreshed. If this is your case, you should also 
     * add header to Http response, that will force browser 
     * not to use cache:
     * Cached elements were not my problem, so the second snippet is not tested by me. For more info, please see following sources: OTN Discussion Forum, Refresh current JSF page and caching on StackOverflow.
     * Note: page refresh done that way means sending Http Response – one Http Request can have only one Http Response. You can’t use this code if you have already sent your response earlier for this particular Http Request.
     * For example – while performing file download. The file download data is sent in Http Response, so unfortunately there is no way to refresh page programmaticaly. Probably Ajax refresh could help you, if just some of the components should be rerendered. This problem is described in threads: StackOverflow thread 1, Coderanch question, another Coderanch problem, refresh after response is commited, and the last one Coderanch - so.. just impossible…
     * 
     */

    public void response() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        System.out.println("RefreshJSFPage: response="+response.toString());
    }
}
