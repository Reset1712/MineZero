## KNOWN ISSUES

- **Issue 2**: If you are on fire when you die, after RBD you will still be on fire.
  - **Status**: Open/In Progress/Resolved
  - **Reproduction Steps**:
    1. Set a checkpoint using `/setCheckpoint`.
    2. Set yourself on fire (e.g., using a Flint and Steel).
    3. Die (e.g., jump from a height or `/kill`).
    4. **Expected Result**: Player respawns at the checkpoint without being on fire.
    5. **Actual Result**: Player respawns at the checkpoint but remains on fire.
  - **Workaround**: Extinguish yourself before dying.
- **Issue 3**: Dying in the nether does not reset you until you go back to nether
  -  **Status**: Open
  - **Reproduction Steps**:
    1. Set a checkpoint in the Overworld using `/setCheckpoint`.
    2. Enter the Nether.
    3. Die in the Nether (e.g., jump from a height or `/kill`).
    4. **Expected Result**: Player respawns at the Overworld checkpoint.
    5. **Actual Result**: Player respawns in the overworld, but cannot mine and does not get inventory back until going to the nether.
  - **Workaround**: Return to the Nether to fix.