/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */


package com.codenvy.analytics.metrics;

import com.codenvy.analytics.metrics.value.ListListStringValueData;
import com.codenvy.analytics.metrics.value.ListStringValueData;
import com.codenvy.analytics.metrics.value.ValueData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public class UsersActivityMetric extends ValueReadBasedMetric {

    UsersActivityMetric() {
        super(MetricType.USER_ACTIVITY);
    }

    /** {@inheritDoc} */
    @Override
    public Set<MetricParameter> getParams() {
        return new LinkedHashSet<MetricParameter>(Arrays.asList(new MetricParameter[]{
                MetricParameter.FROM_DATE,
                MetricParameter.TO_DATE,
                MetricParameter.ALIAS}));
    }

    /** {@inheritDoc} */
    @Override
    protected Class< ? extends ValueData> getValueDataClass() {
        return ListListStringValueData.class;
    }

    /** {@inheritDoc} */
    @Override
    public ValueData getValue(Map<String, String> context) throws IOException {
        ListListStringValueData valueData = (ListListStringValueData) super.getValue(context);
        List<ListStringValueData> newValue = new ArrayList<>(valueData.size());

        for (ListStringValueData item : valueData.getAll()) {
            newValue.add(new ListStringValueData(MessageTransformer.transform(item.getAll().get(0))));
        }

        return new ListListStringValueData(newValue);
    }
}
