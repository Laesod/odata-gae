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


import org.odata4j.producer.jpa.JPAContext;
import org.odata4j.producer.jpa.OEntityToJPAEntityCommand;

public class GAEOEntityToJPAEntityCommand extends OEntityToJPAEntityCommand {
	  private boolean withLinks;
	  private JPAContext.EntityAccessor accessor;

	public GAEOEntityToJPAEntityCommand(boolean withLinks) {
	    this(JPAContext.EntityAccessor.ENTITY, withLinks);
	}

	public GAEOEntityToJPAEntityCommand(JPAContext.EntityAccessor accessor,
			boolean withLinks) {
		super(accessor, withLinks);
		this.accessor = accessor;
	    this.withLinks = withLinks;
	}

	@Override
	public boolean execute(JPAContext context) {

		Object jpaEntity = GAEJPAProducer.createNewJPAEntity(
	        context.getEntityManager(),
	        accessor.getEntity(context).getJPAEntityType(),
	        accessor.getEntity(context).getOEntity(),
	        withLinks);
	    accessor.getEntity(context).setJpaEntity(jpaEntity);

	    return false;
	}
}
