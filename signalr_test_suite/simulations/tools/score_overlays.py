from PIL import Image
import numpy as np

base_img = Image.open('signalr_test_suite/Baku_Formula_One_circuit_map.svg.webp').convert('RGBA')
arr = np.array(base_img)
# Track pixels: dark or colored pixels
is_track = (arr[:, :, 3] > 128) & ((arr[:, :, 0] < 50) | (arr[:, :, 2] > 100))

for fx in [1, -1]:
    for fy in [1, -1]:
        overlay = Image.open(f'signalr_test_suite/debug_overlay_fx{fx}_fy{fy}.png')
        o_arr = np.array(overlay)
        # Red line pixels: R > 200, G < 50, B < 50
        is_red = (o_arr[:, :, 0] > 200) & (o_arr[:, :, 1] < 50) & (o_arr[:, :, 2] < 50)
        
        # Check how many red pixels are on or within 3px of track pixels
        # Dilation of track mask by 3px
        from scipy.ndimage import binary_dilation
        dilated = binary_dilation(is_track, iterations=4)
        overlap = np.sum(is_red & dilated)
        total_red = np.sum(is_red)
        score = overlap / total_red if total_red > 0 else 0
        print(f"fx={fx:2d}, fy={fy:2d}: overlap score = {score:.4f} ({overlap}/{total_red} pixels)")
