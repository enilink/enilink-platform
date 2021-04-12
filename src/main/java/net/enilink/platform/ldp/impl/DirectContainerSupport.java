package net.enilink.platform.ldp.impl;

import com.google.common.collect.ImmutableSet;
import net.enilink.composition.annotations.Precedes;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.*;
import net.enilink.komma.model.IModel;
import net.enilink.komma.rdf4j.RDF4JValueConverter;
import net.enilink.platform.ldp.LDP;
import net.enilink.platform.ldp.LdpDirectContainer;
import net.enilink.platform.ldp.LdpResource;
import net.enilink.platform.ldp.ReqBodyHelper;
import net.enilink.platform.ldp.config.ContainerHandler;
import net.enilink.platform.ldp.config.DirectContainerHandler;
import net.enilink.platform.ldp.config.Handler;
import net.enilink.platform.ldp.config.RdfResourceHandler;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Precedes(RdfSourceSupport.class)
public abstract class DirectContainerSupport implements LdpDirectContainer, Behaviour<LdpDirectContainer> {
	@Override
	public IReference getRelType() {
		return LDP.TYPE_DIRECTCONTAINER;
	}

	@Override
	public Set<IReference> getTypes() {
		return ImmutableSet.of(LDP.TYPE_CONTAINER, LDP.TYPE_DIRECTCONTAINER);
	}

	@Override
	public Map<Boolean, String> update( ReqBodyHelper body,  Handler handler) {
		Set<IStatement> configStmts = null;
		if (body != null && handler != null & (body.isDirectContainer() || handler instanceof DirectContainerHandler) && !body.isBasicContainer() && body.isNoContains()) {
			URI resourceUri = body.getURI();
			IEntityManager manager = getEntityManager();
			Property memberRel = hasMemberRelation();
			LdpResource memberSrc = membershipResource();
			configStmts = matchDirectContainerConfig((DirectContainerHandler)handler, resourceUri);
			manager.removeRecursive(resourceUri, true);
			manager.add(new Statement(resourceUri, RDF.PROPERTY_TYPE, LDP.TYPE_DIRECTCONTAINER));
			hasMemberRelation(memberRel);
			membershipResource(memberSrc);
			configStmts.forEach(stmt -> manager.add(stmt));
			RDF4JValueConverter valueConverter = body.valueConverter();
			body.getRdfBody().forEach(stmt -> {
				IReference subj = valueConverter.fromRdf4j(stmt.getSubject());
				IReference pred = valueConverter.fromRdf4j(stmt.getPredicate());
				IValue obj = valueConverter.fromRdf4j(stmt.getObject());
				boolean acceptable = !(subj == resourceUri && body.isServerProperty(pred)) &&
					!(handler instanceof DirectContainerHandler && (pred == LDP.PROPERTY_HASMEMBERRELATION) || (pred == LDP.PROPERTY_MEMBERSHIPRESOURCE && memberSrc != null));

				if (acceptable)
					manager.add(new Statement(subj, pred, obj));
			});
			manager.add(new Statement(resourceUri, LDP.DCTERMS_PROPERTY_MODIFIED,
					new Literal(Instant.now().toString(), XMLSCHEMA.TYPE_DATETIME)));
			return Collections.singletonMap(true, "");
		}
		return Collections.singletonMap(false, " the resource to be modified is direct container, couldn't be replaced with resource of another type . ");
	}

	private Set<IStatement>  matchDirectContainerConfig(DirectContainerHandler handler, URI resourceUri){
		Set<IStatement> stmts = matchConfig(handler, resourceUri);
		if(null != handler.getMembership())
			stmts.addAll( Arrays.asList(
                 new Statement(resourceUri, LDP.PROPERTY_HASMEMBERRELATION, handler.getMembership()),
                 new Statement(resourceUri, LDP.PROPERTY_MEMBERSHIPRESOURCE, membershipResource())));
		return stmts;
	}

	@Override
	public Map<Boolean,String> createResource(IModel model, URI resourceType, RdfResourceHandler resourceHandler, ContainerHandler containerHandler, ReqBodyHelper body){
		Map<Boolean,String> result = getBehaviourDelegate().createResource( model,  resourceType,  resourceHandler, containerHandler,  body);
		if(result.get(true) != null){
			getEntityManager().add(new Statement(getURI(), LDP.PROPERTY_CONTAINS, body,getURI()));
			URI membershipSrc = membershipResource() != null ? membershipResource().getURI() : null;
			URI membership = null;
			if(null == membershipSrc && containerHandler instanceof DirectContainerHandler) {
				DirectContainerHandler dh = (DirectContainerHandler) containerHandler;
				RdfResourceHandler memSrcConfig = dh.getRelSource();
				if (memSrcConfig != null && memSrcConfig.getAssignedTo() != null)
					membershipSrc = memSrcConfig.getAssignedTo();
				else  membershipSrc = parentUri();
				membership = hasMemberRelation() != null ? hasMemberRelation().getURI() : dh.getMembership();
			}
			if(null != membershipSrc && null != membership){
				//getEntityManager().find(membershipSrc).getEntityManager().add(new Statement(membershipSrc, membership, body.getURI()));
				model.getModelSet().getModel(membershipSrc, true).getManager().add(new Statement(membershipSrc, membership, body.getURI()));
			}
			else return Collections.singletonMap(false, "not valid body entity or configuration fault");
		}
		return result;
	}

	private  URI parentUri(){
		URI requestedUri = getURI();
		if (requestedUri.segmentCount() > 1 && requestedUri.toString().endsWith("/"))
			return requestedUri.trimSegments(2).appendSegment("");
		else if (requestedUri.segmentCount() > 0)
			return requestedUri.trimSegments(1).appendSegment("");
		else return null;
	}
}
