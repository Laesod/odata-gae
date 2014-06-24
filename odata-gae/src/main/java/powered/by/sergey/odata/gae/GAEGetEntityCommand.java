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


import javax.persistence.metamodel.EntityType;

import org.odata4j.exceptions.NotFoundException;
import org.odata4j.producer.jpa.Command;
import org.odata4j.producer.jpa.JPAContext;
import org.odata4j.producer.jpa.JPAContext.EntityAccessor;
import org.odata4j.producer.jpa.JPAResults;

public class GAEGetEntityCommand implements Command {

  private EntityAccessor accessor;

  public GAEGetEntityCommand() {
    this(EntityAccessor.ENTITY);
  }

  public GAEGetEntityCommand(EntityAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  public boolean execute(JPAContext context) {

    EntityType<?> jpaEntityType = accessor.getEntity(context)
        .getJPAEntityType();
    Object typeSafeEntityKey = accessor.getEntity(context)
        .getTypeSafeEntityKey();
    
    Object jpaEntity = null;
    boolean duplicateFieldExeption;
    do {
	    try {
	    	duplicateFieldExeption = false;
	    	jpaEntity = context.getEntityManager().find(
	    			jpaEntityType.getJavaType(), typeSafeEntityKey);
	    } catch (IllegalArgumentException e) {
	    	if (e.getMessage().contains("DuplicateDatastoreFieldException")) {
	    		duplicateFieldExeption = true;
	    	} else {
	    		throw e;
	    	}
	    }
    } while (duplicateFieldExeption);

    if (jpaEntity == null) {
      throw new NotFoundException(jpaEntityType
          .getJavaType()
          + " not found with key "
          + typeSafeEntityKey);
    }

    accessor.getEntity(context).setJpaEntity(jpaEntity);

    context.setResult(JPAResults.entity(accessor
        .getEntity(context).getJpaEntity()));

    return false;
  }
}