package com.exxeta.routing;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;

public class HomePage extends WebPage {

    private static final long serialVersionUID = 1L;
    private final RunnerDataBean bean = new RunnerDataBean();

    public HomePage(final PageParameters parameters) {
        Form<RunnerDataBean> form = new Form<RunnerDataBean>("form", new CompoundPropertyModel<RunnerDataBean>(bean));
        add(form);
        form.setOutputMarkupId(true);

        TextField fc;
        fc = new TextField("lonlat");
        form.add(fc);

        fc = new TextField("distance");
        form.add(fc);

        form.add(new CalculateButton("calc", bean));
    }
}
