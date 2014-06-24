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


import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.jpa.JPAContext;

public class GAEJPAContext extends JPAContext {
	private ContextEntity entity;
	private ContextEntity otherEntity;

	// get entity / with nav property (count?)
	public GAEJPAContext(EdmDataServices metadata, String entitySetName,
			OEntityKey oEntityKey, String navProperty,
			QueryInfo queryInfo) {
		super(metadata, entitySetName, oEntityKey, navProperty, queryInfo);
	    this.entity = new GAEContextEntity(entitySetName, oEntityKey, null);
	}

	// create
	public GAEJPAContext(EdmDataServices metadata, String entitySetName,
			OEntityKey oEntityKey, String navProperty, OEntity oEntity) {
		super(metadata, entitySetName, oEntityKey, navProperty, oEntity);
	    this.entity = new GAEContextEntity(entitySetName, oEntityKey, null);
	    this.otherEntity = new GAEContextEntity(oEntity.getEntitySetName(), oEntity.getEntityKey(), oEntity);
	}

	// update, merge, delete
	protected GAEJPAContext(EdmDataServices metadata, String entitySetName,
			OEntityKey oEntityKey, OEntity oEntity) {
		super(metadata, entitySetName, oEntityKey, oEntity);
	    this.entity = new GAEContextEntity(entitySetName, oEntityKey, oEntity);
	}

	// query
	public GAEJPAContext(EdmDataServices metadata, String entitySetName,
			QueryInfo queryInfo) {
		super(metadata, entitySetName, queryInfo);
		this.entity = new GAEContextEntity(entitySetName, null, null);
	}

	@Override
	public ContextEntity getEntity() {
		return entity;
	}

	@Override
	public ContextEntity getOtherEntity() {
		return otherEntity;
	}
    
    public class GAEContextEntity extends ContextEntity {
        private OEntityKey oEntityKey;

		public GAEContextEntity(String entitySetName, OEntityKey oEntityKey,
				OEntity oEntity) {
			super(entitySetName, oEntityKey, oEntity);
			this.oEntityKey = oEntityKey;
		}
    	
		@Override
	    public Object getTypeSafeEntityKey() {
	        return GAEJPAProducer.typeSafeEntityKey(
	            getEntityManager(),
	            getJPAEntityType(),
	            oEntityKey);
	    }
    }
}
