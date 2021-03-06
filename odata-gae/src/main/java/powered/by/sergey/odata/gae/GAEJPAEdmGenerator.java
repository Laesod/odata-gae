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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.ManyToMany;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.core4j.Enumerable;
import org.core4j.Predicate1;
import org.odata4j.core.OFuncs;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmAssociationSet;
import org.odata4j.edm.EdmAssociationSetEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDecorator;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.producer.jpa.JPAEdmGenerator;
import org.odata4j.producer.jpa.JPAMember;

public class GAEJPAEdmGenerator extends JPAEdmGenerator {
	private final Logger log = Logger.getLogger(getClass().getName());

	public GAEJPAEdmGenerator(EntityManagerFactory emf, String namespace) {
		super(emf, namespace);
	}

	@Override
	protected List<EdmProperty.Builder> getProperties(String modelNamespace,
			ManagedType<?> et) {
		List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		for (Attribute<?, ?> att : et.getAttributes()) {

			if (att.isCollection() || att instanceof PluralAttribute) {
			} else {
				SingularAttribute<?, ?> sa = (SingularAttribute<?, ?>) att;

				Type<?> type = sa.getType();
				// Do we have an embedded composite key here? If so, we have to
				// flatten the @EmbeddedId since
				// only any set of non-nullable, immutable, <EDMSimpleType>
				// declared properties MAY serve as the key.
				if (sa.isId()
						&& type.getPersistenceType() == PersistenceType.EMBEDDABLE) {
					properties.addAll(getProperties(modelNamespace,
							(ManagedType<?>) sa.getType()));
				} else if (type.getPersistenceType().equals(
						PersistenceType.BASIC)
						|| type.getPersistenceType().equals(
								PersistenceType.EMBEDDABLE)) {
					EdmProperty.Builder prop = toEdmProperty(modelNamespace, sa);
					properties.add(prop);
				}
			}
		}
		return properties;
	}

	@Override
	public EdmDataServices.Builder generateEdm(EdmDecorator decorator) {

		String modelNamespace = getModelSchemaNamespace();

		List<EdmEntityType.Builder> edmEntityTypes = new ArrayList<EdmEntityType.Builder>();
		List<EdmComplexType.Builder> edmComplexTypes = new ArrayList<EdmComplexType.Builder>();
		List<EdmAssociation.Builder> associations = new ArrayList<EdmAssociation.Builder>();

		List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();
		List<EdmAssociationSet.Builder> associationSets = new ArrayList<EdmAssociationSet.Builder>();

		Metamodel mm = getEntityManagerFactory().getMetamodel();

		// complex types
		for (EmbeddableType<?> et : mm.getEmbeddables()) {

			String name = et.getJavaType().getSimpleName();
			List<EdmProperty.Builder> properties = getProperties(
					modelNamespace, et);

			EdmComplexType.Builder ect = EdmComplexType.newBuilder()
					.setNamespace(modelNamespace).setName(name)
					.addProperties(properties);
			edmComplexTypes.add(ect);
		}

		// entities
		for (EntityType<?> et : mm.getEntities()) {

			String name = getEntitySetName(et);

			List<String> keys = new ArrayList<String>();
			SingularAttribute<?, ?> idAttribute = null;
			if (et.hasSingleIdAttribute()) {
				idAttribute = getIdAttribute(et);
				// handle composite embedded keys (@EmbeddedId)
				if (idAttribute.getPersistentAttributeType() == PersistentAttributeType.EMBEDDED) {
					keys = Enumerable
							.create(getProperties(modelNamespace,
									(ManagedType<?>) idAttribute.getType()))
							.select(OFuncs.name(EdmProperty.Builder.class))
							.toList();
				} else {
					keys = Enumerable.create(idAttribute.getName()).toList();
				}
			} else {
				throw new IllegalArgumentException("IdClass not yet supported");
			}

			List<EdmProperty.Builder> properties = getProperties(
					modelNamespace, et);
			List<EdmNavigationProperty.Builder> navigationProperties = new ArrayList<EdmNavigationProperty.Builder>();

			EdmEntityType.Builder eet = EdmEntityType.newBuilder()
					.setNamespace(modelNamespace).setName(name).addKeys(keys)
					.addProperties(properties)
					.addNavigationProperties(navigationProperties);
			edmEntityTypes.add(eet);

			EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(name)
					.setEntityType(eet);
			entitySets.add(ees);
		}

		Map<String, EdmEntityType.Builder> eetsByName = Enumerable.create(
				edmEntityTypes).toMap(OFuncs.name(EdmEntityType.Builder.class));
		Map<String, EdmEntitySet.Builder> eesByName = Enumerable.create(
				entitySets).toMap(OFuncs.name(EdmEntitySet.Builder.class));

		// associations + navigationproperties on the non-collection side
		for (EntityType<?> et : mm.getEntities()) {

			for (Attribute<?, ?> att : et.getAttributes()) {

				if (!att.isCollection() && !(att instanceof PluralAttribute)) {

					SingularAttribute<?, ?> singularAtt = (SingularAttribute<?, ?>) att;

					Type<?> singularAttType = singularAtt.getType();
					if (singularAttType.getPersistenceType().equals(
							PersistenceType.ENTITY)) {

						// we found a single attribute to an entity
						// create an edm many-to-one relationship (* 0..1)
						EntityType<?> singularAttEntityType = (EntityType<?>) singularAttType;

						EdmEntityType.Builder fromEntityType = eetsByName
								.get(getEntitySetName(et));
						EdmEntityType.Builder toEntityType = eetsByName
								.get(getEntitySetName(singularAttEntityType));

						// add EdmAssociation, EdmAssociationSet
						EdmAssociation.Builder association = defineManyTo(
								EdmMultiplicity.ZERO_TO_ONE, associations,
								fromEntityType, toEntityType, modelNamespace,
								eesByName, associationSets);

						// add EdmNavigationProperty
						EdmNavigationProperty.Builder navigationProperty = EdmNavigationProperty
								.newBuilder(singularAtt.getName())
								.setRelationship(association)
								.setFromTo(association.getEnd1(),
										association.getEnd2());
						fromEntityType
								.addNavigationProperties(navigationProperty);
					}
				}
			}
		}

		// navigation properties for the collection side of associations
		for (EntityType<?> et : mm.getEntities()) {

			for (Attribute<?, ?> att : et.getAttributes()) {

				if (att.isCollection()) {

					// one-to-many
					PluralAttribute<?, ?, ?> pluralAtt = (PluralAttribute<?, ?, ?>) att;
					JPAMember m = JPAMember.create(pluralAtt, null);
					ManyToMany manyToMany = m.getAnnotation(ManyToMany.class);

					EntityType<?> pluralAttEntityType = (EntityType<?>) pluralAtt
							.getElementType();

					final EdmEntityType.Builder fromEntityType = eetsByName
							.get(getEntitySetName(et));
					final EdmEntityType.Builder toEntityType = eetsByName
							.get(getEntitySetName(pluralAttEntityType));

					try {
						EdmAssociation.Builder association = Enumerable.create(
								associations).firstOrNull(
								new Predicate1<EdmAssociation.Builder>() {
									public boolean apply(
											EdmAssociation.Builder input) {
										return input
												.getEnd1()
												.getType()
												.getFullyQualifiedTypeName()
												.equals(toEntityType
														.getFullyQualifiedTypeName())
												&& input.getEnd2()
														.getType()
														.getFullyQualifiedTypeName()
														.equals(fromEntityType
																.getFullyQualifiedTypeName());
									}
								});

						EdmAssociationEnd.Builder fromRole, toRole;

						if (association == null) {
							// no EdmAssociation, EdmAssociationSet defined,
							// backfill!

							// add EdmAssociation, EdmAssociationSet
							if (manyToMany != null) {
								association = defineManyTo(
										EdmMultiplicity.MANY, associations,
										fromEntityType, toEntityType,
										modelNamespace, eesByName,
										associationSets);
								fromRole = association.getEnd1();
								toRole = association.getEnd2();
							} else {
								association = defineManyTo(
										EdmMultiplicity.ZERO_TO_ONE,
										associations, toEntityType,
										fromEntityType, modelNamespace,
										eesByName, associationSets);
								fromRole = association.getEnd2();
								toRole = association.getEnd1();
							}

						} else {
							fromRole = association.getEnd2();
							toRole = association.getEnd1();
						}

						// add EdmNavigationProperty
						EdmNavigationProperty.Builder navigationProperty = EdmNavigationProperty
								.newBuilder(pluralAtt.getName())
								.setRelationship(association)
								.setFromTo(fromRole, toRole);
						fromEntityType
								.addNavigationProperties(navigationProperty);

					} catch (Exception e) {
						log.log(Level.WARNING,
								"Exception building Edm associations: "
										+ e.getMessage(), e);
					}
				}

			}

		}

		EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder()
				.setName(getEntityContainerName()).setIsDefault(true)
				.addEntitySets(entitySets).addAssociationSets(associationSets);

		EdmSchema.Builder modelSchema = EdmSchema.newBuilder()
				.setNamespace(modelNamespace).addEntityTypes(edmEntityTypes)
				.addComplexTypes(edmComplexTypes).addAssociations(associations);
		EdmSchema.Builder containerSchema = EdmSchema.newBuilder()
				.setNamespace(getContainerSchemaNamespace())
				.addEntityContainers(container);

		return EdmDataServices.newBuilder().addSchemas(containerSchema,
				modelSchema);
	}

	private EdmAssociation.Builder defineManyTo(EdmMultiplicity toMult,
			List<EdmAssociation.Builder> associations,
			EdmEntityType.Builder fromEntityType,
			EdmEntityType.Builder toEntityType, String modelNamespace,
			Map<String, EdmEntitySet.Builder> eesByName,
			List<EdmAssociationSet.Builder> associationSets) {
		EdmMultiplicity fromMult = EdmMultiplicity.MANY;

		String assocName = getAssociationName(associations, fromEntityType,
				toEntityType);

		// add EdmAssociation
		EdmAssociationEnd.Builder fromAssociationEnd = EdmAssociationEnd
				.newBuilder().setRole(fromEntityType.getName())
				.setType(fromEntityType).setMultiplicity(fromMult);
		String toAssociationEndName = toEntityType.getName();
		if (toAssociationEndName.equals(fromEntityType.getName())) {
			toAssociationEndName = toAssociationEndName + "1";
		}
		EdmAssociationEnd.Builder toAssociationEnd = EdmAssociationEnd
				.newBuilder().setRole(toAssociationEndName)
				.setType(toEntityType).setMultiplicity(toMult);
		EdmAssociation.Builder association = EdmAssociation.newBuilder()
				.setNamespace(modelNamespace).setName(assocName)
				.setEnds(fromAssociationEnd, toAssociationEnd);
		associations.add(association);

		// add EdmAssociationSet
		EdmEntitySet.Builder fromEntitySet = eesByName.get(fromEntityType
				.getName());
		EdmEntitySet.Builder toEntitySet = eesByName
				.get(toEntityType.getName());
		EdmAssociationSet.Builder associationSet = EdmAssociationSet
				.newBuilder()
				.setName(assocName)
				.setAssociation(association)
				.setEnds(
						EdmAssociationSetEnd.newBuilder()
								.setRole(fromAssociationEnd)
								.setEntitySet(fromEntitySet),
						EdmAssociationSetEnd.newBuilder()
								.setRole(toAssociationEnd)
								.setEntitySet(toEntitySet));
		associationSets.add(associationSet);

		return association;
	}

}
