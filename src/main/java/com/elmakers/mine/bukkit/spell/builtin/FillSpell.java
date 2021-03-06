package com.elmakers.mine.bukkit.spell.builtin;

import com.elmakers.mine.bukkit.api.block.BrushMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import com.elmakers.mine.bukkit.api.block.MaterialBrush;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.spell.TargetType;
import com.elmakers.mine.bukkit.batch.FillBatch;
import com.elmakers.mine.bukkit.spell.BrushSpell;

public class FillSpell extends BrushSpell 
{
	private static final int DEFAULT_MAX_DIMENSION = 128;
	
	private Block targetBlock = null;

	@Override
	public SpellResult onCast(ConfigurationSection parameters) 
	{
		boolean singleBlock = getTargetType() != TargetType.SELECT;
		Block targetBlock = getTargetBlock();
        if (!singleBlock && parameters.getBoolean("select_self", true) && isLookingDown())
        {
            targetBlock = mage.getLocation().getBlock().getRelative(BlockFace.DOWN);
        }
		if (targetBlock == null) 
		{
			return SpellResult.NO_TARGET;
		}

        MaterialBrush buildWith = getBrush();
        boolean hasPermission = buildWith != null && buildWith.isErase() ? hasBreakPermission(targetBlock) : hasBuildPermission(targetBlock);
        if (!hasPermission) {
			return SpellResult.INSUFFICIENT_PERMISSION;
		}

		if (singleBlock)
		{
			deactivate();
            if (isIndestructible(targetBlock))
            {
                return SpellResult.NO_TARGET;
            }

			registerForUndo(targetBlock);

			buildWith.setTarget(targetBlock.getLocation());
			buildWith.update(mage, targetBlock.getLocation());
			buildWith.modify(targetBlock);
			
			controller.updateBlock(targetBlock);
			registerForUndo();
			
			return SpellResult.CAST;
		}
		
		if (targetLocation2 != null) {
			this.targetBlock = targetLocation2.getBlock();
		}

		if (this.targetBlock != null)
		{
			// Update the brush using the center of the fill volume.
            // This is kind of a hack to make map-building easier
			Location centerLocation = this.targetBlock.getLocation();
            Location secondLocation = this.targetBlock.getLocation();
            if (buildWith.getMode() == BrushMode.MAP) {
                centerLocation = targetBlock.getLocation();
                centerLocation.setX(Math.floor((centerLocation.getX() + secondLocation.getX()) / 2));
                centerLocation.setY(Math.floor((centerLocation.getY() + secondLocation.getY()) / 2));
                centerLocation.setZ(Math.floor((centerLocation.getZ() + secondLocation.getZ()) / 2));
            }
			buildWith.setTarget(this.targetBlock.getLocation(), centerLocation);
			
			FillBatch batch = new FillBatch(this, secondLocation, targetBlock.getLocation(), buildWith);

			int maxDimension = parameters.getInt("max_dimension", DEFAULT_MAX_DIMENSION);	
			maxDimension = parameters.getInt("md", maxDimension);	
			maxDimension = (int)(mage.getConstructionMultiplier() * maxDimension);
			
			if (!batch.checkDimension(maxDimension))
			{
				return SpellResult.FAIL;
			}
			boolean success = mage.addBatch(batch);
			
			deactivate();
			return success ? SpellResult.CAST : SpellResult.FAIL;
		}
		else
		{
			this.targetBlock = targetBlock;
			activate();
			return SpellResult.TARGET_SELECTED;
		}
	}

	@Override
	protected boolean isBatched() {
		return true;
	}

	@Override
	public boolean onCancel()
	{
		if (targetBlock != null)
		{
			// Extra set here, just in case we're not in sync with active state.
			targetBlock = null;
			deactivate();
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onDeactivate() {
		targetBlock = null;
	}
}
