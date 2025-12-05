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