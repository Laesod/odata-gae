package powered.by.sergey.odata.gae;

/*
 * #%L
 * ProjectX2013_03_23_web
 * %%
 * Copyright (C) 2013 - 2014 Powered by Sergey
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

import org.odata4j.core.OEntity;
import org.odata4j.producer.jpa.Command;
import org.odata4j.producer.jpa.JPAContext;

public class GAEMergeEntityCommand implements Command {

  @Override
  public boolean execute(JPAContext context) {
    EntityManager em = context.getEntityManager();
    EntityType<?> jpaEntityType = context.getEntity()
        .getJPAEntityType();
    Object jpaEntity = context.getEntity().getJpaEntity();
    OEntity entity = context.getEntity().getOEntity();

    GAEJPAProducer.applyOProperties(em, jpaEntityType,
        entity.getProperties(),
        jpaEntity);
    GAEJPAProducer.applyOLinks(em, jpaEntityType, entity.getLinks(),
        jpaEntity);

    return false;
  }
}
