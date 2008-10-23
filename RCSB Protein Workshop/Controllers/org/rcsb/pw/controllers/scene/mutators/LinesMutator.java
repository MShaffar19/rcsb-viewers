package org.rcsb.pw.controllers.scene.mutators;

import java.awt.Color;
import java.util.Vector;

import org.rcsb.mbt.controllers.app.AppBase;
import org.rcsb.mbt.glscene.jogl.DisplayListRenderable;
import org.rcsb.mbt.glscene.jogl.JoglSceneNode;
import org.rcsb.mbt.glscene.jogl.LineGeometry;
import org.rcsb.mbt.model.Atom;
import org.rcsb.mbt.model.Bond;
import org.rcsb.mbt.model.Chain;
import org.rcsb.mbt.model.Fragment;
import org.rcsb.mbt.model.LineSegment;
import org.rcsb.mbt.model.ExternChain;
import org.rcsb.mbt.model.Residue;
import org.rcsb.mbt.model.Structure;
import org.rcsb.mbt.model.StructureComponent;
import org.rcsb.mbt.model.StructureComponentRegistry;
import org.rcsb.mbt.model.StructureMap;
import org.rcsb.mbt.model.attributes.LineStyle;
import org.rcsb.mbt.model.geometry.Algebra;
import org.rcsb.mbt.model.util.PdbToNdbConverter;
import org.rcsb.pw.controllers.app.ProteinWorkshop;
import org.rcsb.pw.controllers.scene.mutators.options.LinesOptions;




public class LinesMutator extends Mutator
{
	private LinesOptions options = null;
	public Vector<LineSegment> lines = new Vector<LineSegment>();
	
	private boolean isFirstClick = true;	// Is this the first object in an object pair? Else this is the second click.
	public LinesMutator()
	{
		super();
		this.options = new LinesOptions();
	}

	
	@Override
	public boolean supportsBatchMode()
	{
		return false;
	}
	
	
	@Override
	public void doMutationSingle(final Object mutee) {
		
		if(mutee instanceof LineSegment)
		{
			JoglSceneNode sn = (JoglSceneNode)AppBase.sgetModel().getStructures().get(0).getStructureMap().getUData();
			sn.removeRenderable((LineSegment)mutee);
			sn.removeLabel(mutee);
		}
		
		else
		{
			if(this.isFirstClick)
			{
				this.getOptions().setFirstPoint(this.getCoordinates(mutee));
				final String message = this.reportAtComponent(mutee);
				this.getOptions().setFirstDescription(message);
				ProteinWorkshop.sgetActiveFrame().getLinesPanel().updateObject1Text(this.getOptions().getFirstDescription());
			}
			
			else 
			{
				this.getOptions().setSecondPoint(this.getCoordinates(mutee));
				final String message = this.reportAtComponent(mutee);
				this.getOptions().setSecondDescription(message);
				ProteinWorkshop.sgetActiveFrame().getLinesPanel().updateObject2Text(this.getOptions().getSecondDescription());
				this.drawLine();
			}
		}
		
		this.isFirstClick = !this.isFirstClick;
	}
	
	public void drawLine() {
		final LineSegment line = new LineSegment(this.getOptions().getFirstPoint(), this.getOptions().getSecondPoint());
		final LineGeometry geometry = new LineGeometry();
		final LineStyle style = new LineStyle();
		style.lineStyle = this.options.getLineStyle();
		style.label = Algebra.distance(line.getFirstPoint().vector, line.getSecondPoint().vector) + "";
		// 5 characters max
		style.label = style.label.substring(0, style.label.length() >= 5 ? 5 : style.label.length());
		
		final StructureMap sm = AppBase.sgetModel().getStructures().get(0).getStructureMap();
		sm.getStructureStyles().setStyle(line, style);
		System.arraycopy(this.getOptions().getColor(), 0, style.getColor(), 0, style.getColor().length);
		((JoglSceneNode)sm.getUData()).addRenderable(new DisplayListRenderable( line, style, geometry ));
		
		lines.add(line);
	}
	

	public String reportAtComponent(final Object mutee) {
		String message = null;
		final PdbToNdbConverter converter = AppBase.sgetModel().getStructures().get(0).getStructureMap().getPdbToNdbConverter();
		
		if(mutee instanceof StructureComponent) {
			final StructureComponent structureComponent = (StructureComponent)mutee;
			final String scType = structureComponent.getStructureComponentType();
			
			if (scType == StructureComponentRegistry.TYPE_ATOM) {
				final Atom atom = (Atom) structureComponent;
	
				final Object[] pdbIds = AppBase.sgetModel().getStructures().get(0).getStructureMap().getPdbToNdbConverter().getPdbIds(
						atom.chain_id, new Integer(atom.residue_id));
				if (pdbIds != null) {
					final String pdbResidueId = (String) pdbIds[1];
	
					message = "Atom: " + atom.name
							+ ", residue " + pdbResidueId + ", compound "
							+ atom.compound + ", chain ";
				} else {
					message =
									"Atom: "
											+ atom.chain_id + "/" 
											+ atom.residue_id + "/" 
											+ atom.name	+ "/"
											+ atom.compound;
				}
			}
			
			else if (scType == StructureComponentRegistry.TYPE_BOND)
			{
				final Bond bond = (Bond) structureComponent;
	
				final Atom a1 = bond.getAtom(0);
				final Atom a2 = bond.getAtom(1);
	
				Object[] tmp = converter.getPdbIds(a1.chain_id, new Integer(a1.residue_id));
				final Object[] tmp2 = converter.getPdbIds(a2.chain_id, new Integer(a2.residue_id));
				if (tmp != null && tmp2 != null) {
					final String a1ChainId = (String) tmp[0];
					final String a1ResidueId = (String) tmp[1];
					tmp = converter.getPdbIds(a2.chain_id, new Integer(a2.residue_id));
					final String a2ChainId = (String) tmp2[0];
					final String a2ResidueId = (String) tmp2[1];
	
					message = "Covalent bond. Atom 1: "
							+ a1.name + ", res: " + a1ResidueId + ", chain "
							+ a1ChainId + "; Atom 2: " + a2.name + ", res "
							+ a2ResidueId + ", chain " + a2ChainId;
				}
				
				else
				{
					message =
									"Covalent bond. Atom 1: "
											+ a1.name
											+ ", res* "
											+ a1.residue_id
											+ ", chain* "
											+ a1.chain_id
											+ "; Atom 2: "
											+ a2.name
											+ ", res* "
											+ a2.residue_id
											+ ", chain* "
											+ a2.chain_id;
				}
			}
			
			else if (scType == StructureComponentRegistry.TYPE_RESIDUE)
			{
				final Residue r = (Residue) structureComponent;
				final Fragment f = r.getFragment();
				final String conformationTypeIdentifier = f.getConformationType();
				String conformationType = "Unknown";
				if (conformationTypeIdentifier == StructureComponentRegistry.TYPE_COIL) {
					conformationType = "Coil";
				} else if (conformationTypeIdentifier == StructureComponentRegistry.TYPE_HELIX) {
					conformationType = "Helix";
				} else if (conformationTypeIdentifier == StructureComponentRegistry.TYPE_STRAND) {
					conformationType = "Strand";
				} else if (conformationTypeIdentifier == StructureComponentRegistry.TYPE_TURN) {
					conformationType = "Turn";
				}
	
				final Object[] tmp = converter.getPdbIds(r.getChainId(), new Integer(r.getResidueId()));
				if (tmp != null)
				{
					final String pdbChainId = (String) tmp[0];
					final String pdbResidueId = (String) tmp[1];
	
					message = "Residue " + pdbResidueId
							+ ", from chain " + pdbChainId + "; "
							+ conformationType + " conformation; "
							+ r.getCompoundCode() + " compound.";
				}
				
				else
				{
					message = "Res* "
											+ r.getResidueId()
											+ ", from chain "
											+ r.getChainId()
											+ "; "
											+ conformationType
											+ " conf; "
											+ r.getCompoundCode()
											+ " comp";
				}
			}
			
			else if (scType == StructureComponentRegistry.TYPE_FRAGMENT)
			{
				final Fragment f = (Fragment) structureComponent;
	
				// remove all but the local name for the secondary structure class.
				String conformation = f.getConformationType();
				final int lastDot = conformation.lastIndexOf('.');
				conformation = conformation.substring(lastDot + 1);
	
				final String ndbChainId = f.getChain().getChainId();
				final int startNdbResidueId = f.getResidue(0).getResidueId();
				final int endNdbResidueId = f.getResidue(f.getResidueCount() - 1)
						.getResidueId();
	
				final Object[] tmp = converter.getPdbIds(
						ndbChainId, new Integer(startNdbResidueId));
				final Object[] tmp2 = converter.getPdbIds(
						ndbChainId, new Integer(endNdbResidueId));
	
				if (tmp != null && tmp2 != null) {
					final String startPdbChainId = (String) tmp[0];
					final String startPdbResidueId = (String) tmp[1];
					final String endPdbResidueId = (String) tmp2[1];
	
					message = conformation
							+ " fragment: chain " + startPdbChainId
							+ ", " + startPdbResidueId
							+ " - " + endPdbResidueId;
				} else {
					message = conformation
											+ " fragment: chain* "
											+ f.getChain().getChainId()
											+ ", "
											+ startNdbResidueId
											+ " - "
											+ endNdbResidueId;
				}
	
				// structureMap.getStructureStyles().setResidueColor(res, yellow);
			} else if (scType == StructureComponentRegistry.TYPE_CHAIN) {
				final Chain c = (Chain) structureComponent;
	
				// remove all but the local name for the secondary structure class.
				final String pdbChainId = converter.getFirstPdbChainId(c.getChainId());
				if (pdbChainId != null) {
					message = "Chain " + pdbChainId
							+ " backbone";
				} else {
					message = "Chain "
											+ c.getChainId();
				}
	
				// structureMap.getStructureStyles().setResidueColor(res, yellow);
			}
			
			else if (mutee instanceof ExternChain)
			{
				final ExternChain c = (ExternChain) structureComponent;
	
				// remove all but the local name for the secondary structure class.
				message = (c.isBasicChain())? "Chain " + c.getChainId() : 
						  (c.isWaterChain())? "Water Molecules" :
							  "Miscellaneous Molecules (no chain ID)";
			} 
			
		} else if(mutee instanceof Structure){	// not a StructureComponent
			final Structure struct = (Structure)mutee;
			message = struct.toString();
		}
		
		return message;
	}
	
	public double[] getCoordinates(final Object mutee) {
		final double[] coordinates = {0,0,0};
		
		if(mutee instanceof StructureComponent) {
			final StructureComponent structureComponent = (StructureComponent)mutee;
			final String scType = structureComponent.getStructureComponentType();
			
			if (scType == StructureComponentRegistry.TYPE_ATOM) {
				final Atom atom = (Atom) structureComponent;
	
				System.arraycopy(atom.coordinate, 0, coordinates, 0, atom.coordinate.length);
			} else if (scType == StructureComponentRegistry.TYPE_BOND) {
				final Bond bond = (Bond) structureComponent;
	
				final Atom a1 = bond.getAtom(0);
				final Atom a2 = bond.getAtom(1);
	
				// the point half way along the bond.
				for(int i = 0; i < a1.coordinate.length; i++) {
					coordinates[i] = (a1.coordinate[i] + a2.coordinate[i]) / 2;
				}
			} else if (scType == StructureComponentRegistry.TYPE_RESIDUE) {
				final Residue r = (Residue) structureComponent;
				Atom atom;
				if(r.getAlphaAtomIndex() >= 0) {
					atom = r.getAlphaAtom();
				} else {
					atom = r.getAtom(r.getAtomCount() / 2);
				}
				
				return this.getCoordinates(atom);
			} else if (scType == StructureComponentRegistry.TYPE_FRAGMENT) {
				final Fragment f = (Fragment) structureComponent;
				final Residue r = f.getResidue(f.getResidueCount() / 2);
				
				return this.getCoordinates(r);
			} else if (scType == StructureComponentRegistry.TYPE_CHAIN) {
				final Chain c = (Chain) structureComponent;
				final Residue r = c.getResidue(c.getResidueCount() / 2);
				
				return this.getCoordinates(r);
			} else if(structureComponent instanceof ExternChain) {
				final ExternChain c = (ExternChain) structureComponent;
				final Residue r = c.getResidue(c.getResidueCount() / 2);
				
				return this.getCoordinates(r); 
			}
		} else if(mutee instanceof Structure){	// not a StructureComponent
			final Structure struct = (Structure)mutee;
			final StructureMap sm = struct.getStructureMap();
			final Residue r = sm.getResidue(sm.getResidueCount() / 2);
			
			return this.getCoordinates(r);
		}
		
		return coordinates;
	}

	
	
	@Override
	public void doMutation()
	{
		for (Object next : mutees)
			this.doMutationSingle(next);
	}
	
	public LinesOptions getOptions() {
		return this.options;
	}	
	
	
	public void reset() {
		this.isFirstClick = true;
		Color.WHITE.getComponents(this.options.getColor());
	}
	
	
	@Override
	public void clearStructure() {
		this.reset();
	}
}
